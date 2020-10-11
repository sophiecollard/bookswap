package com.github.sophiecollard.bookswap.services.transaction.copyrequest

import java.time.ZoneId

import cats.implicits._
import cats.{Monad, ~>}
import com.github.sophiecollard.bookswap.authorization._
import com.github.sophiecollard.bookswap.authorization.instances.{ByActiveStatus, WithAuthorizationByActiveStatus}
import com.github.sophiecollard.bookswap.domain.inventory.{Copy, CopyStatus}
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.RequestStatus._
import com.github.sophiecollard.bookswap.domain.transaction.{CopyRequest, CopyRequestPagination, RequestStatus}
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.repositories.inventory.CopyRepository
import com.github.sophiecollard.bookswap.repositories.transaction.CopyRequestRepository
import com.github.sophiecollard.bookswap.services.error.ServiceError.{FailedToCreateResource, FailedToUpdateResource, ResourceNotFound}
import com.github.sophiecollard.bookswap.services.error.{ServiceError, ServiceErrorOr}
import com.github.sophiecollard.bookswap.services.transaction.copyrequest.authorization._
import com.github.sophiecollard.bookswap.services.transaction.copyrequest.state.StateUpdate._
import com.github.sophiecollard.bookswap.services.transaction.copyrequest.state._
import com.github.sophiecollard.bookswap.syntax._

trait CopyRequestService[F[_]] {

  import CopyRequestService.RequestAndCopy

  /** Fetches a CopyRequest */
  def get(id: Id[CopyRequest]): F[ServiceErrorOr[CopyRequest]]

  /** Fetches a list of CopyRequests for the specified Copy */
  def listForCopy(copyId: Id[Copy], pagination: CopyRequestPagination): F[List[CopyRequest]]

  /** Invoked by a registered user to list the CopyRequests he/she issued */
  def listForRequester(pagination: CopyRequestPagination)(userId: Id[User]): F[List[CopyRequest]]

  /** Invoked by a registered user to create a new CopyRequest */
  def create(copyId: Id[Copy])(userId: Id[User]): F[WithAuthorizationByActiveStatus[ServiceErrorOr[CopyRequest]]]

  /** Invoked by a registered user to cancel one of their CopyRequests */
  def cancel(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByRequestIssuer[ServiceErrorOr[RequestAndCopy]]]

  /** Invoked by the Copy owner to accept a CopyRequest */
  def accept(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[ServiceErrorOr[RequestAndCopy]]]

  /** Invoked by the Copy owner to reject CopyRequest */
  def reject(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[ServiceErrorOr[RequestAndCopy]]]

  /** Invoked by the Copy owner to mark a CopyRequest as fulfilled */
  def markAsFulfilled(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[ServiceErrorOr[RequestAndCopy]]]

}

object CopyRequestService {

  type RequestAndCopy = (CopyRequest, Copy)
  type Statuses = (RequestStatus, CopyStatus)

  type AuthorizationInput = (Id[User], Id[CopyRequest])

  def create[F[_]: Monad, G[_]: Monad](
    authorizationByActiveStatus: AuthorizationService[F, Id[User], ByActiveStatus],
    authorizationByRequestIssuer: AuthorizationService[F, AuthorizationInput, ByRequestIssuer],
    authorizationByCopyOwner: AuthorizationService[F, AuthorizationInput, ByCopyOwner],
    copyRequestRepository: CopyRequestRepository[G],
    copyRepository: CopyRepository[G],
    transactor: G ~> F
  )(
    implicit zoneId: ZoneId // TODO Include in config object
  ): CopyRequestService[F] = {
    new CopyRequestService[F] {
      override def get(id: Id[CopyRequest]): F[ServiceErrorOr[CopyRequest]] =
        copyRequestRepository
          .get(id)
          .orElse[ServiceError](ResourceNotFound("CopyRequest", id))
          .transact(transactor)

      override def listForCopy(copyId: Id[Copy], pagination: CopyRequestPagination): F[List[CopyRequest]] =
        copyRequestRepository
          .listForCopy(copyId, pagination)
          .transact(transactor)

      override def listForRequester(pagination: CopyRequestPagination)(userId: Id[User]): F[List[CopyRequest]] =
        copyRequestRepository
          .listForRequester(userId, pagination)
          .transact(transactor)

      override def create(copyId: Id[Copy])(userId: Id[User]): F[WithAuthorizationByActiveStatus[ServiceErrorOr[CopyRequest]]] =
        authorizationByActiveStatus.authorize(userId) {
          val copyRequest = CopyRequest(
            id = Id.generate[CopyRequest],
            copyId,
            requestedBy = userId,
            requestedOn = now,
            status = Pending
          )

          copyRequestRepository
            .create(copyRequest)
            .ifTrue(copyRequest)
            .orElse[ServiceError](FailedToCreateResource("CopyRequest", copyRequest.id))
            .transact(transactor)
        }

      override def cancel(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByRequestIssuer[ServiceErrorOr[RequestAndCopy]]] =
        authorizationByRequestIssuer.authorize((userId, requestId)) {
          handleCommand(requestId)(StateMachine.handleCancelCommand)
        }

      override def accept(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[ServiceErrorOr[RequestAndCopy]]] =
        authorizationByCopyOwner.authorize((userId, requestId)) {
          handleCommand(requestId)(StateMachine.handleAcceptCommand)
        }

      override def reject(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[ServiceErrorOr[RequestAndCopy]]] =
        authorizationByCopyOwner.authorize((userId, requestId)) {
          handleCommand(requestId)(StateMachine.handleRejectCommand)
        }

      override def markAsFulfilled(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[ServiceErrorOr[RequestAndCopy]]] =
        authorizationByCopyOwner.authorize((userId, requestId)) {
          handleCommand(requestId)(StateMachine.handleMarkAsFulfilledCommand)
        }

      private def handleCommand(
        requestId: Id[CopyRequest]
      )(
        handler: InitialState => Either[InvalidState, StateUpdate]
      ): F[ServiceErrorOr[RequestAndCopy]] =
        (
          for {
            (copyRequest, copy, maybeNextCopyRequest) <- (
              for {
                copyRequest <- copyRequestRepository
                  .get(requestId)
                  .asEitherT[ServiceError](ResourceNotFound("CopyRequest", requestId))
                copy <- copyRepository
                  .get(copyRequest.copyId)
                  .asEitherT[ServiceError](ResourceNotFound("Copy", copyRequest.copyId))
                maybeNextCopyRequest <- copyRequestRepository
                  .findFirstOnWaitingList(copy.id)
                  .liftToEitherT[ServiceError]
              } yield (copyRequest, copy, maybeNextCopyRequest)
            ).mapK[F](transactor)
            initialState = InitialState(copyRequest.status, maybeNextCopyRequest.map(r =>(r.id, r.status)), copy.status)
            (updatedRequestStatus, updatedCopyStatus) <- handler(initialState)
              .pure[F]
              .asEitherT
              .leftMap[ServiceError](_ => ServiceError.InvalidState(s"Invalid state: $initialState"))
              .flatMapF(performStateUpdate(copyRequest.id, copy.id, initialState))
            updatedRequest = copyRequest.copy(status = updatedRequestStatus)
            updatedCopy = copy.copy(status = updatedCopyStatus)
          } yield (updatedRequest, updatedCopy)
        ).value

      private def performStateUpdate(
        requestId: Id[CopyRequest],
        copyId: Id[Copy],
        initialState: InitialState
      )(
        stateUpdate: StateUpdate
      ): F[Either[ServiceError, Statuses]] = {
        val maybeStatuses = stateUpdate match {
          case UpdateRequestStatus(requestStatus) =>
            copyRequestRepository.updateStatus(requestId, requestStatus) ifTrue
              (requestStatus, initialState.copyStatus)
          case UpdateRequestAndNextRequestStatuses(requestStatus, nextRequestId, nextRequestStatus) =>
            copyRequestRepository.updateStatus(requestId, requestStatus) >>
              copyRequestRepository.updateStatus(nextRequestId, nextRequestStatus) ifTrue
              (requestStatus, initialState.copyStatus)
          case UpdateRequestAndCopyStatuses(requestStatus, copyStatus) =>
            copyRequestRepository.updateStatus(requestId, requestStatus) >>
              copyRepository.updateStatus(copyId, copyStatus) ifTrue
              (requestStatus, copyStatus)
          case UpdateRequestAndOpenRequestsAndCopyStatuses(requestStatus, openRequestsStatus, copyStatus) =>
            copyRequestRepository.updateStatus(requestId, requestStatus) >>
              copyRequestRepository.updatePendingRequestsStatuses(copyId, openRequestsStatus) >>
              copyRequestRepository.updateWaitingListRequestsStatuses(copyId, openRequestsStatus) >>
              copyRepository.updateStatus(copyId, copyStatus) ifTrue
              (requestStatus, copyStatus)
          case NoUpdate =>
            (initialState.requestStatus, initialState.copyStatus)
              .some.pure[G]
        }

        maybeStatuses
          .orElse[ServiceError](FailedToUpdateResource("CopyRequest", requestId))
          .transact(transactor)
      }
    }
  }

}

package com.github.sophiecollard.bookswap.services.transaction.copyrequest

import java.time.ZoneId

import cats.{Monad, ~>}
import cats.implicits._
import com.github.sophiecollard.bookswap.domain.inventory.{Copy, CopyStatus}
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.RequestStatus._
import com.github.sophiecollard.bookswap.domain.transaction.{CopyRequest, RequestStatus}
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.error.ServiceError.{FailedToCreateResource, FailedToUpdateResource, ResourceNotFound}
import com.github.sophiecollard.bookswap.error.{ServiceError, ServiceErrorOr}
import com.github.sophiecollard.bookswap.repositories.inventory.CopyRepository
import com.github.sophiecollard.bookswap.repositories.transaction.CopyRequestRepository
import com.github.sophiecollard.bookswap.services.authorization.Instances.{ByActiveStatus, WithAuthorizationByActiveStatus}
import com.github.sophiecollard.bookswap.services.authorization._
import com.github.sophiecollard.bookswap.services.syntax._
import com.github.sophiecollard.bookswap.services.transaction.copyrequest.Authorization._
import com.github.sophiecollard.bookswap.services.transaction.copyrequest.state.StateUpdate._
import com.github.sophiecollard.bookswap.services.transaction.copyrequest.state._
import com.github.sophiecollard.bookswap.syntax.EitherTSyntax._
import com.github.sophiecollard.bookswap.syntax.JavaTimeSyntax.now

trait CopyRequestService[F[_]] {

  import CopyRequestService.Statuses

  /** Fetches a CopyRequest */
  def get(id: Id[CopyRequest]): F[ServiceErrorOr[CopyRequest]]

  /** Invoked by a registered user to create a new CopyRequest */
  def create(copyId: Id[Copy])(userId: Id[User]): F[WithAuthorizationByActiveStatus[ServiceErrorOr[CopyRequest]]]

  /** Invoked by a registered user to cancel one of their CopyRequests */
  def cancel(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByRequestIssuer[ServiceErrorOr[Statuses]]]

  /** Invoked by the Copy owner to accept a CopyRequest */
  def accept(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[ServiceErrorOr[Statuses]]]

  /** Invoked by the Copy owner to reject CopyRequest */
  def reject(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[ServiceErrorOr[Statuses]]]

  /** Invoked by the Copy owner to mark a CopyRequest as fulfilled */
  def markAsFulfilled(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[ServiceErrorOr[Statuses]]]

}

object CopyRequestService {

  type Statuses = (RequestStatus, CopyStatus)

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

      override def cancel(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByRequestIssuer[ServiceErrorOr[Statuses]]] =
        authorizationByRequestIssuer.authorize(AuthorizationInput(userId, requestId)) {
          handleCommand(requestId)(StateMachine.handleCancelCommand)
        }

      override def accept(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[ServiceErrorOr[Statuses]]] =
        authorizationByCopyOwner.authorize(AuthorizationInput(userId, requestId)) {
          handleCommand(requestId)(StateMachine.handleAcceptCommand)
        }

      override def reject(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[ServiceErrorOr[Statuses]]] =
        authorizationByCopyOwner.authorize(AuthorizationInput(userId, requestId)) {
          handleCommand(requestId)(StateMachine.handleRejectCommand)
        }

      override def markAsFulfilled(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[ServiceErrorOr[Statuses]]] =
        authorizationByCopyOwner.authorize(AuthorizationInput(userId, requestId)) {
          handleCommand(requestId)(StateMachine.handleMarkAsFulfilledCommand)
        }

      private def handleCommand(
        requestId: Id[CopyRequest]
      )(
        handler: InitialState => Either[InvalidState, StateUpdate]
      ): F[ServiceErrorOr[Statuses]] =
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
            statuses <- handler(initialState)
              .pure[F]
              .asEitherT
              .leftMap[ServiceError](_ => ServiceError.InvalidState(s"Invalid state: $initialState"))
              .flatMapF(performStateUpdate(copyRequest.id, copy.id, initialState))
          } yield statuses
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

package com.github.sophiecollard.bookswap.services.transaction.copyrequest

import java.time.ZoneId

import cats.{Monad, ~>}
import cats.implicits._
import com.github.sophiecollard.bookswap.domain.inventory.{Copy, CopyStatus}
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.RequestStatus._
import com.github.sophiecollard.bookswap.domain.transaction.{CopyRequest, RequestStatus}
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.error.Error.ServiceError.ResourceNotFound
import com.github.sophiecollard.bookswap.error.Error.{ServiceError, ServiceErrorOr}
import com.github.sophiecollard.bookswap.repositories.inventory.CopyRepository
import com.github.sophiecollard.bookswap.repositories.transaction.CopyRequestRepository
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
  def create(copyId: Id[Copy])(userId: Id[User]): F[ServiceErrorOr[CopyRequest]]

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
    requestIssuerAuthorizationService: AuthorizationService[F, AuthorizationInput, ByRequestIssuer],
    copyOwnerAuthorizationService: AuthorizationService[F, AuthorizationInput, ByCopyOwner],
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
          .map(_.toRight[ServiceError](ResourceNotFound("CopyRequest", id)))
          .transact(transactor)

      override def create(copyId: Id[Copy])(userId: Id[User]): F[ServiceErrorOr[CopyRequest]] = {
        val copyRequest = CopyRequest(
          id = Id.generate[CopyRequest],
          copyId,
          requestedBy = userId,
          requestedOn = now,
          status = Pending
        )

        copyRequestRepository
          .create(copyRequest)
          .transact(transactor)
          .map(_ => copyRequest.asRight[ServiceError])
      }

      override def cancel(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByRequestIssuer[ServiceErrorOr[Statuses]]] =
        requestIssuerAuthorizationService.authorize(AuthorizationInput(userId, requestId)) {
          handleCommand(requestId)(StateMachine.handleCancelCommand)
        }

      override def accept(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[ServiceErrorOr[Statuses]]] =
        copyOwnerAuthorizationService.authorize(AuthorizationInput(userId, requestId)) {
          handleCommand(requestId)(StateMachine.handleAcceptCommand)
        }

      override def reject(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[ServiceErrorOr[Statuses]]] =
        copyOwnerAuthorizationService.authorize(AuthorizationInput(userId, requestId)) {
          handleCommand(requestId)(StateMachine.handleRejectCommand)
        }

      override def markAsFulfilled(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[ServiceErrorOr[Statuses]]] =
        copyOwnerAuthorizationService.authorize(AuthorizationInput(userId, requestId)) {
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
              .semiflatMap(performStateUpdate(copyRequest.id, copy.id, initialState))
          } yield statuses
        ).value

      private def performStateUpdate(
        requestId: Id[CopyRequest],
        copyId: Id[Copy],
        initialState: InitialState
      )(
        stateUpdate: StateUpdate
      ): F[Statuses] =
        (
          stateUpdate match {
            case UpdateRequestStatus(requestStatus) =>
              copyRequestRepository.updateStatus(requestId, requestStatus) as
                (requestStatus, initialState.copyStatus)
            case UpdateRequestAndNextRequestStatuses(requestStatus, nextRequestId, nextRequestStatus) =>
              copyRequestRepository.updateStatus(requestId, requestStatus) >>
                copyRequestRepository.updateStatus(nextRequestId, nextRequestStatus) as
                (requestStatus, initialState.copyStatus)
            case UpdateRequestAndCopyStatuses(requestStatus, copyStatus) =>
              copyRequestRepository.updateStatus(requestId, requestStatus) >>
                copyRepository.updateStatus(copyId, copyStatus) as
                (requestStatus, copyStatus)
            case UpdateRequestAndOpenRequestsAndCopyStatuses(requestStatus, openRequestsStatus, copyStatus) =>
              copyRequestRepository.updateStatus(requestId, requestStatus) >>
                copyRequestRepository.updatePendingRequestsStatuses(copyId, openRequestsStatus) >>
                copyRequestRepository.updateWaitingListRequestsStatuses(copyId, openRequestsStatus) >>
                copyRepository.updateStatus(copyId, copyStatus) as
                (requestStatus, copyStatus)
            case NoUpdate =>
              (initialState.requestStatus, initialState.copyStatus)
                .pure[G]
          }
        ).transact(transactor)
    }
  }

}

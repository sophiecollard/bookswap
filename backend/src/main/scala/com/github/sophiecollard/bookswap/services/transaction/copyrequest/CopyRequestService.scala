package com.github.sophiecollard.bookswap.services.transaction.copyrequest

import java.time.ZoneId

import cats.Monad
import cats.implicits._
import com.github.sophiecollard.bookswap.domain.inventory.{Copy, CopyStatus}
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.RequestStatus._
import com.github.sophiecollard.bookswap.domain.transaction.{CopyRequest, RequestStatus}
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.error.Error
import com.github.sophiecollard.bookswap.error.Error.{ResourceNotFound, TransactionError, TransactionErrorOr}
import com.github.sophiecollard.bookswap.repositories.inventory.CopyRepository
import com.github.sophiecollard.bookswap.repositories.transaction.CopyRequestRepository
import com.github.sophiecollard.bookswap.services.authorization._
import com.github.sophiecollard.bookswap.services.transaction.copyrequest.Authorization._
import com.github.sophiecollard.bookswap.services.transaction.copyrequest.state.StateUpdate.{NoUpdate, UpdateRequestAndCopyStatuses, UpdateRequestAndNextRequestStatuses, UpdateRequestStatus}
import com.github.sophiecollard.bookswap.services.transaction.copyrequest.state.{InvalidState, InitialState, StateMachine, StateUpdate}
import com.github.sophiecollard.bookswap.syntax.EitherTSyntax._
import com.github.sophiecollard.bookswap.syntax.JavaTimeSyntax.now

trait CopyRequestService[F[_]] {

  import CopyRequestService.Statuses

  /**
    * Invoked by a registered user to create a new CopyRequest.
    */
  def create(copyId: Id[Copy])(userId: Id[User]): F[TransactionErrorOr[CopyRequest]]

  /**
    * Invoked by a registered user to cancel one of their CopyRequests.
    */
  def cancel(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByRequestIssuer[TransactionErrorOr[Statuses]]]

  /**
    * Invoked by the Copy owner to accept a CopyRequest.
    */
  def accept(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[TransactionErrorOr[Statuses]]]

  /**
    * Invoked by the Copy owner to reject CopyRequest.
    */
  def reject(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[TransactionErrorOr[Statuses]]]

  /**
    * Invoked by the Copy owner to mark a CopyRequest as fulfilled.
    */
  def markAsFulfilled(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[TransactionErrorOr[Statuses]]]

}

object CopyRequestService {

  type Statuses = (RequestStatus, CopyStatus)

  def create[F[_]: Monad](
    requestIssuerAuthorizationService: AuthorizationService[F, AuthorizationInput, ByRequestIssuer],
    copyOwnerAuthorizationService: AuthorizationService[F, AuthorizationInput, ByCopyOwner],
    copyRequestRepository: CopyRequestRepository[F],
    copyRepository: CopyRepository[F]
  )(
    implicit zoneId: ZoneId // TODO Include in config object
  ): CopyRequestService[F] = {
    new CopyRequestService[F] {
      override def create(copyId: Id[Copy])(userId: Id[User]): F[TransactionErrorOr[CopyRequest]] = {
        val copyRequest = CopyRequest(
          id = Id.generate[CopyRequest],
          copyId,
          requestedBy = userId,
          requestedOn = now,
          status = Pending
        )

        copyRequestRepository
          .create(copyRequest)
          .map(_ => copyRequest.asRight[TransactionError])
      }

      override def cancel(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByRequestIssuer[TransactionErrorOr[Statuses]]] =
        requestIssuerAuthorizationService.authorize(AuthorizationInput(userId, requestId)) {
          handleCommand(requestId)(StateMachine.handleCancelCommand)
        }

      override def accept(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[TransactionErrorOr[Statuses]]] =
        copyOwnerAuthorizationService.authorize(AuthorizationInput(userId, requestId)) {
          handleCommand(requestId)(StateMachine.handleAcceptCommand)
        }

      override def reject(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[TransactionErrorOr[Statuses]]] =
        copyOwnerAuthorizationService.authorize(AuthorizationInput(userId, requestId)) {
          handleCommand(requestId)(StateMachine.handleRejectCommand)
        }

      override def markAsFulfilled(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[TransactionErrorOr[Statuses]]] =
        copyOwnerAuthorizationService.authorize(AuthorizationInput(userId, requestId)) {
          handleCommand(requestId)(StateMachine.handleMarkAsFulfilledCommand)
        }

      private def handleCommand(
        requestId: Id[CopyRequest]
      )(
        handler: InitialState => Either[InvalidState, StateUpdate]
      ): F[TransactionErrorOr[Statuses]] =
        (
          for {
            copyRequest <- copyRequestRepository
              .get(requestId)
              .asEitherT[TransactionError](ResourceNotFound("CopyRequest", requestId))
            copy <- copyRepository
              .get(copyRequest.copyId)
              .asEitherT[TransactionError](ResourceNotFound("Copy", copyRequest.copyId))
            maybeNextCopyRequest <- copyRequestRepository
              .findFirstOnWaitingList(copy.id)
              .liftToEitherT[TransactionError]
            initialState = InitialState(copyRequest.status, maybeNextCopyRequest.map(r =>(r.id, r.status)), copy.status)
            statuses <- handler(initialState)
              .pure[F]
              .asEitherT
              .leftMap[TransactionError](_ => Error.InvalidState(s"Invalid state: $initialState"))
              .flatMapF(performStateUpdate(copyRequest.id, copy.id, initialState))
          } yield statuses
          ).value

      private def performStateUpdate(
        requestId: Id[CopyRequest],
        copyId: Id[Copy],
        initialState: InitialState
      )(
        stateUpdate: StateUpdate
      ): F[TransactionErrorOr[Statuses]] = {
        stateUpdate match {
          case UpdateRequestStatus(requestStatus) =>
            copyRequestRepository.updateStatus(requestId, requestStatus) as
              Right((requestStatus, initialState.copyStatus))
          case UpdateRequestAndNextRequestStatuses(requestStatus, nextRequestId, nextRequestStatus) =>
            copyRequestRepository.updateStatus(requestId, requestStatus) >>
              copyRequestRepository.updateStatus(nextRequestId, nextRequestStatus) as
              Right((requestStatus, initialState.copyStatus))
          case UpdateRequestAndCopyStatuses(requestStatus, copyStatus) =>
            copyRequestRepository.updateStatus(requestId, requestStatus) >>
              copyRepository.updateStatus(copyId, copyStatus) as
              Right((requestStatus, copyStatus))
          case NoUpdate =>
            (initialState.requestStatus, initialState.copyStatus)
              .asRight[TransactionError]
              .pure[F]
        }
      }
    }
  }

}

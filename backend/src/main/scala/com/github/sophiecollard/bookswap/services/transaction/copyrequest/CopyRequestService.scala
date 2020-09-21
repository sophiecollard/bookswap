package com.github.sophiecollard.bookswap.services.transaction.copyrequest

import java.time.ZoneId

import cats.Monad
import cats.implicits._
import com.github.sophiecollard.bookswap.domain.inventory.{Copy, CopyStatus}
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.{CopyRequest, RequestStatus}
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.error.Error.{ResourceNotFound, TransactionError, TransactionErrorOr}
import com.github.sophiecollard.bookswap.repositories.inventory.CopyRepository
import com.github.sophiecollard.bookswap.repositories.transaction.CopyRequestRepository
import com.github.sophiecollard.bookswap.services.authorization._
import com.github.sophiecollard.bookswap.services.transaction.copyrequest.Authorization._
import com.github.sophiecollard.bookswap.syntax.EitherTSyntax.{FOpToEitherT, FToEitherT}
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

  sealed trait Command
  case object Accept          extends Command
  case object Reject          extends Command
  case object MarkAsFulfilled extends Command

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
          status = RequestStatus.Pending
        )

        copyRequestRepository
          .create(copyRequest)
          .map(_ => copyRequest.asRight[TransactionError])
      }

      override def cancel(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByRequestIssuer[TransactionErrorOr[Statuses]]] =
        requestIssuerAuthorizationService.authorize(AuthorizationInput(userId, requestId)) {
          (
            for {
              copyRequest <- copyRequestRepository
                .get(requestId)
                .asEitherT(ResourceNotFound("CopyRequest", requestId))
              copy <- copyRepository
                .get(copyRequest.copyId)
                .asEitherT(ResourceNotFound("Copy", copyRequest.copyId))
              // Update the current CopyRequest's status
              updatedRequestStatus = RequestStatus.cancelled(now)
              _ <- copyRequestRepository
                .updateStatus(requestId, updatedRequestStatus)
                .liftToEitherT[TransactionError]
              // Update the status of the first CopyRequest on the waiting list, if any
              // If no CopyRequest is found on the waiting list, the Copy's status changes back to Available
              updatedCopyStatus <- copyRequestRepository.findFirstOnWaitingList(copy.id).flatMap {
                case Some(nextRequestOnWaitingList) =>
                  copyRequestRepository
                    .updateStatus(nextRequestOnWaitingList.id, RequestStatus.Accepted(now))
                    .as[CopyStatus](CopyStatus.Reserved)
                case None =>
                  copyRepository
                    .updateStatus(copy.id, CopyStatus.Available)
                    .as[CopyStatus](CopyStatus.Available)
              }.liftToEitherT[TransactionError]
            } yield (updatedRequestStatus, updatedCopyStatus)
          ).value
        }

      override def accept(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[TransactionErrorOr[Statuses]]] =
        copyOwnerAuthorizationService.authorize(AuthorizationInput(userId, requestId)) {
          (RequestStatus.accepted(now), CopyStatus.Reserved: CopyStatus)
            .asRight[TransactionError]
            .pure[F]
        }

      override def reject(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[TransactionErrorOr[Statuses]]] =
        copyOwnerAuthorizationService.authorize(AuthorizationInput(userId, requestId)) {
          (RequestStatus.rejected(now), CopyStatus.Available: CopyStatus)
            .asRight[TransactionError]
            .pure[F]
        }

      override def markAsFulfilled(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[TransactionErrorOr[Statuses]]] =
        copyOwnerAuthorizationService.authorize(AuthorizationInput(userId, requestId)) {
          (RequestStatus.fulfilled(now), CopyStatus.Swapped: CopyStatus)
            .asRight[TransactionError]
            .pure[F]
        }
    }
  }

}

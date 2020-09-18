package com.github.sophiecollard.bookswap.services.transaction.copyrequest

import java.time.ZoneId

import cats.Monad
import cats.implicits._
import com.github.sophiecollard.bookswap.domain.inventory.{CopyOnOffer, CopyOnOfferStatus}
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.{CopyRequest, RequestStatus}
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.error.Error.{ResourceNotFound, TransactionError, TransactionErrorOr}
import com.github.sophiecollard.bookswap.repositories.inventory.CopyOnOfferRepository
import com.github.sophiecollard.bookswap.repositories.transaction.CopyRequestRepository
import com.github.sophiecollard.bookswap.services.authorization._
import com.github.sophiecollard.bookswap.services.transaction.copyrequest.Authorization._
import com.github.sophiecollard.bookswap.syntax.EitherTSyntax.{FOpToEitherT, FToEitherT}
import com.github.sophiecollard.bookswap.syntax.JavaTimeSyntax.now

trait CopyRequestService[F[_]] {

  import CopyRequestService.Command

  /**
    * Invoked by a registered user to create a new CopyRequest.
    */
  def create(copyId: Id[CopyOnOffer])(userId: Id[User]): F[TransactionErrorOr[CopyRequest]]

  /**
    * Invoked by a registered user to cancel one of their CopyRequests.
    */
  def cancel(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByRequestIssuer[TransactionErrorOr[RequestStatus]]]

  /**
    * Invoked by the CopyOnOffer owner to accept, reject or complete a CopyRequest.
    */
  def respond(requestId: Id[CopyRequest], command: Command)(userId: Id[User]): F[WithAuthorizationByCopyOwner[TransactionErrorOr[RequestStatus]]]

}

object CopyRequestService {

  sealed trait Command
  case object Accept          extends Command
  case object Reject          extends Command
  case object MarkAsCompleted extends Command

  def create[F[_]: Monad](
    requestIssuerAuthorizationService: AuthorizationService[F, AuthorizationInput, ByRequestIssuer],
    copyOwnerAuthorizationService: AuthorizationService[F, AuthorizationInput, ByCopyOwner],
    copyRequestRepository: CopyRequestRepository[F],
    copyOnOfferRepository: CopyOnOfferRepository[F]
  )(
    implicit zoneId: ZoneId // TODO Include in config object
  ): CopyRequestService[F] = {
    new CopyRequestService[F] {
      override def create(copyId: Id[CopyOnOffer])(userId: Id[User]): F[TransactionErrorOr[CopyRequest]] = {
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

      override def cancel(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByRequestIssuer[TransactionErrorOr[RequestStatus]]] =
        requestIssuerAuthorizationService.authorize(AuthorizationInput(userId, requestId)) {
          (
            for {
              copyRequest <- copyRequestRepository
                .get(requestId)
                .asEitherT(ResourceNotFound("CopyRequest", requestId))
              copyOnOffer <- copyOnOfferRepository
                .get(copyRequest.copyId)
                .asEitherT(ResourceNotFound("CopyOnOffer", copyRequest.copyId))
              // Update the current CopyRequest's status
              updatedRequestStatus = RequestStatus.cancelled(now)
              _ <- copyRequestRepository
                .updateStatus(requestId, updatedRequestStatus)
                .liftToEitherT[TransactionError]
              // Update the status of the first CopyRequest on the waiting list, if any
              // If no CopyRequest is found on the waiting list, the CopyOnOffer's status changes back to Available
              _ <- copyRequestRepository.findFirstOnWaitingList(copyOnOffer.id).flatMap {
                case Some(nextRequestOnWaitingList) =>
                  copyRequestRepository.updateStatus(nextRequestOnWaitingList.id, RequestStatus.Accepted(now))
                case None =>
                  copyOnOfferRepository.updateStatus(copyOnOffer.id, CopyOnOfferStatus.Available)
              }.liftToEitherT[TransactionError]
            } yield updatedRequestStatus
          ).value
        }

      override def respond(requestId: Id[CopyRequest], command: Command)(userId: Id[User]): F[WithAuthorizationByCopyOwner[TransactionErrorOr[RequestStatus]]] =
        copyOwnerAuthorizationService.authorize(AuthorizationInput(userId, requestId)) {
          // TODO implement
          val status = command match {
            case Accept           => RequestStatus.accepted(now)
            case Reject           => RequestStatus.rejected(now)
            case MarkAsCompleted  => RequestStatus.completed(now)
          }

          status
            .asRight[TransactionError]
            .pure[F]
        }
    }
  }

}

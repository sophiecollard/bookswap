package com.github.sophiecollard.bookswap.services.transaction.copyrequest

import java.time.ZoneId

import cats.Applicative
import cats.implicits._
import com.github.sophiecollard.bookswap.domain.inventory.CopyOnOffer
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.{CopyRequest, RequestStatus}
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.error.Error.{TransactionError, TransactionErrorOr}
import com.github.sophiecollard.bookswap.repositories.inventory.CopyOnOfferRepository
import com.github.sophiecollard.bookswap.repositories.transaction.CopyRequestRepository
import com.github.sophiecollard.bookswap.services.authorization._
import com.github.sophiecollard.bookswap.services.transaction.copyrequest.Authorization._
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

  def create[F[_]: Applicative](
    requestIssuerAuthorizationService: AuthorizationService[F, AuthorizationInput, ByRequestIssuer],
    copyOwnerAuthorizationService: AuthorizationService[F, AuthorizationInput, ByCopyOwner],
    copyRequestRepository: CopyRequestRepository[F],
    copyOnOfferRepository: CopyOnOfferRepository[F]
  )(
    implicit zoneId: ZoneId // TODO Include in config object
  ): CopyRequestService[F] = {
    new CopyRequestService[F] {
      override def create(copyId: Id[CopyOnOffer])(userId: Id[User]): F[TransactionErrorOr[CopyRequest]] =
        // TODO implement
        CopyRequest(
          id = Id.generate[CopyRequest],
          copyId,
          requestedBy = userId,
          requestedOn = now,
          status = RequestStatus.Pending
        )
          .asRight[TransactionError]
          .pure[F]

      override def cancel(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByRequestIssuer[TransactionErrorOr[RequestStatus]]] =
        requestIssuerAuthorizationService.authorize(AuthorizationInput(userId, requestId)) {
          // TODO implement
          RequestStatus
            .cancelled(now)
            .asRight[TransactionError]
            .pure[F]
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

package com.github.sophiecollard.bookswap.service.transaction

import java.time.ZoneId

import cats.{Applicative, Monad}
import cats.implicits._
import com.github.sophiecollard.bookswap.domain.inventory.CopyOnOffer
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.{CopyRequest, RequestStatus}
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.error.Error.{NotTheCopyOwner, NotTheRequestIssuer, TransactionError, TransactionErrorOr}
import com.github.sophiecollard.bookswap.repositories.{CopyOnOfferRepository, CopyRequestRepository}
import com.github.sophiecollard.bookswap.service.authorization._
import com.github.sophiecollard.bookswap.syntax.MonadTransformerSyntax.OptionTSyntax
import com.github.sophiecollard.bookswap.syntax.JavaTimeSyntax.now

trait CopyRequestService[F[_]] {

  import CopyRequestService.{WithAuthorizationByCopyOwner, WithAuthorizationByRequestIssuer}

  def create(copyId: Id[CopyOnOffer])(userId: Id[User]): F[TransactionErrorOr[CopyRequest]]

  def cancel(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByRequestIssuer[TransactionErrorOr[RequestStatus]]]

  def accept(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[TransactionErrorOr[RequestStatus]]]

  def reject(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[TransactionErrorOr[RequestStatus]]]

  def putOnWaitingList(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[TransactionErrorOr[RequestStatus]]]

  def complete(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[TransactionErrorOr[RequestStatus]]]

}

object CopyRequestService {

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

      override def accept(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[TransactionErrorOr[RequestStatus]]] =
        copyOwnerAuthorizationService.authorize(AuthorizationInput(userId, requestId)) {
          // TODO implement
          RequestStatus
            .accepted(now)
            .asRight[TransactionError]
            .pure[F]
        }

      override def reject(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[TransactionErrorOr[RequestStatus]]] =
        copyOwnerAuthorizationService.authorize(AuthorizationInput(userId, requestId)) {
          // TODO implement
          RequestStatus
            .rejected(now)
            .asRight[TransactionError]
            .pure[F]
        }

      override def putOnWaitingList(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[TransactionErrorOr[RequestStatus]]] =
        copyOwnerAuthorizationService.authorize(AuthorizationInput(userId, requestId)) {
          // TODO implement
          RequestStatus
            .onWaitingList(now)
            .asRight[TransactionError]
            .pure[F]
        }

      override def complete(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorizationByCopyOwner[TransactionErrorOr[RequestStatus]]] =
        copyOwnerAuthorizationService.authorize(AuthorizationInput(userId, requestId)) {
          // TODO implement
          RequestStatus
            .completed(now)
            .asRight[TransactionError]
            .pure[F]
        }
    }
  }

  trait ByRequestIssuer
  trait ByCopyOwner

  type WithAuthorizationByRequestIssuer[R] = WithAuthorization[R, ByRequestIssuer]
  type WithAuthorizationByCopyOwner[R] = WithAuthorization[R, ByCopyOwner]

  final case class AuthorizationInput(userId: Id[User], copyRequestId: Id[CopyRequest])

  def createCopyOwnerAuthorizationService[F[_]: Monad](
    copyRequestRepository: CopyRequestRepository[F],
    copyOnOfferRepository: CopyOnOfferRepository[F]
  ): AuthorizationService[F, AuthorizationInput, ByCopyOwner] =
    AuthorizationService.create[F, AuthorizationInput, ByCopyOwner] { case AuthorizationInput(userId, copyRequestId) =>
      val maybeCopyOwnerId = for {
        copyRequest <- copyRequestRepository.get(copyRequestId).asOptionT
        copy <- copyOnOfferRepository.get(copyRequest.copyId).asOptionT
      } yield copy.offeredBy

      maybeCopyOwnerId.value.map {
        case Some(copyOwnerId) if copyOwnerId == userId =>
          Right(())
        case _ =>
          Left(NotTheCopyOwner(userId, copyRequestId))
      }
    }

  def createRequestIssuerAuthorizationService[F[_]: Monad](
    copyRequestRepository: CopyRequestRepository[F]
  ): AuthorizationService[F, AuthorizationInput, ByRequestIssuer] =
    AuthorizationService.create[F, AuthorizationInput, ByRequestIssuer] { case AuthorizationInput(userId, copyRequestId) =>
      copyRequestRepository.get(copyRequestId).map {
        case Some(copyRequest) if copyRequest.requestedBy == userId =>
          Right(())
        case _ =>
          Left(NotTheRequestIssuer(userId, copyRequestId))
      }
    }

}

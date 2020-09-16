package com.github.sophiecollard.bookswap.service.transaction

import java.time.{LocalDateTime, ZoneId}

import cats.{Applicative, Monad}
import cats.implicits._
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.{CopyRequest, RequestStatus}
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.error.Error.{NoPermissionOnCopyRequest, TransactionError, TransactionErrorOr}
import com.github.sophiecollard.bookswap.repositories.{CopyOnOfferRepository, CopyRequestRepository}
import com.github.sophiecollard.bookswap.service.authorization._
import com.github.sophiecollard.bookswap.syntax.MonadTransformerSyntax.OptionTSyntax
import com.github.sophiecollard.bookswap.syntax.JavaTimeSyntax.now

trait CopyRequestService[F[_]] {

  def acceptCopyRequest(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorization[TransactionErrorOr[RequestStatus]]]

  def rejectCopyRequest(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorization[TransactionErrorOr[RequestStatus]]]

  def putCopyRequestOnWaitingList(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorization[TransactionErrorOr[RequestStatus]]]

}

object CopyRequestService {

  final case class AuthorizationInput(userId: Id[User], copyRequestId: Id[CopyRequest])

  def createAuthorizationService[F[_]: Monad](
    copyRequestRepository: CopyRequestRepository[F],
    copyOnOfferRepository: CopyOnOfferRepository[F]
  ): AuthorizationService[F, AuthorizationInput] =
    AuthorizationService.create[F, AuthorizationInput] { case AuthorizationInput(userId, copyRequestId) =>
      val test = for {
        copyRequest <- copyRequestRepository.get(copyRequestId).asOptionT
        copy <- copyOnOfferRepository.get(copyRequest.copyId).asOptionT
      } yield copy.offeredBy == userId

      test.value.map {
        case Some(true) =>
          Right(())
        case Some(false) | None =>
          Left(NoPermissionOnCopyRequest(userId, copyRequestId))
      }
    }

  def create[F[_]: Applicative](
    authorizationService: AuthorizationService[F, AuthorizationInput],
    copyRequestRepository: CopyRequestRepository[F],
    copyOnOfferRepository: CopyOnOfferRepository[F]
  )(
    implicit zoneId: ZoneId // TODO Include in config object
  ): CopyRequestService[F] = {
    new CopyRequestService[F] {
      override def acceptCopyRequest(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorization[TransactionErrorOr[RequestStatus]]] =
        authorizationService.authorize(AuthorizationInput(userId, requestId)) {
          // TODO implement
          RequestStatus
            .accepted(now)
            .asRight[TransactionError]
            .pure[F]
        }

      override def rejectCopyRequest(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorization[TransactionErrorOr[RequestStatus]]] =
        authorizationService.authorize(AuthorizationInput(userId, requestId)) {
          // TODO implement
          RequestStatus
            .rejected(now)
            .asRight[TransactionError]
            .pure[F]
        }

      override def putCopyRequestOnWaitingList(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorization[TransactionErrorOr[RequestStatus]]] =
        authorizationService.authorize(AuthorizationInput(userId, requestId)) {
          // TODO implement
          RequestStatus
            .onWaitingList(now)
            .asRight[TransactionError]
            .pure[F]
        }
    }
  }

}

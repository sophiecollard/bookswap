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

trait CopyRequestHandler[F[_]] {

  def acceptCopyRequest(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorization[TransactionErrorOr[RequestStatus]]]

  def rejectCopyRequest(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorization[TransactionErrorOr[RequestStatus]]]

  def placeCopyRequestOnWaitingList(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorization[TransactionErrorOr[RequestStatus]]]

}

object CopyRequestHandler {

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
    repository: CopyOnOfferRepository[F]
  ): CopyRequestHandler[F] = {
    val zoneId = ZoneId.of("UTC") // TODO pass in configuration
    new CopyRequestHandler[F] {
      override def acceptCopyRequest(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorization[TransactionErrorOr[RequestStatus]]] =
        authorizationService.authorize(AuthorizationInput(userId, requestId)) {
          // TODO implement
          RequestStatus
            .accepted(LocalDateTime.now(zoneId))
            .asRight[TransactionError]
            .pure[F]
        }

      override def rejectCopyRequest(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorization[TransactionErrorOr[RequestStatus]]] =
        authorizationService.authorize(AuthorizationInput(userId, requestId)) {
          // TODO implement
          RequestStatus
            .rejected(LocalDateTime.now(zoneId))
            .asRight[TransactionError]
            .pure[F]
        }

      override def placeCopyRequestOnWaitingList(requestId: Id[CopyRequest])(userId: Id[User]): F[WithAuthorization[TransactionErrorOr[RequestStatus]]] =
        authorizationService.authorize(AuthorizationInput(userId, requestId)) {
          // TODO implement
          RequestStatus
            .waitingList(1, LocalDateTime.now(zoneId))
            .asRight[TransactionError]
            .pure[F]
        }
    }
  }

}

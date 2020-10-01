package com.github.sophiecollard.bookswap.services.transaction.copyrequest

import cats.Monad
import cats.implicits._
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.CopyRequest
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.error.Error.AuthorizationError.{NotTheRequestedCopyOwner, NotTheRequestIssuer}
import com.github.sophiecollard.bookswap.repositories.inventory.CopyRepository
import com.github.sophiecollard.bookswap.repositories.transaction.CopyRequestRepository
import com.github.sophiecollard.bookswap.services.authorization.{AuthorizationService, WithAuthorization}
import com.github.sophiecollard.bookswap.syntax.OptionTSyntax.FOpToOptionT

object Authorization {

  trait ByRequestIssuer
  trait ByCopyOwner

  type WithAuthorizationByRequestIssuer[R] = WithAuthorization[R, ByRequestIssuer]
  type WithAuthorizationByCopyOwner[R] = WithAuthorization[R, ByCopyOwner]

  final case class AuthorizationInput(userId: Id[User], copyRequestId: Id[CopyRequest])

  def byCopyOwner[F[_]: Monad](
    copyRequestRepository: CopyRequestRepository[F],
    copyRepository: CopyRepository[F]
  ): AuthorizationService[F, AuthorizationInput, ByCopyOwner] =
    AuthorizationService.create[F, AuthorizationInput, ByCopyOwner] { case AuthorizationInput(userId, copyRequestId) =>
      val maybeCopyOwnerId = for {
        copyRequest <- copyRequestRepository.get(copyRequestId).asOptionT
        copy <- copyRepository.get(copyRequest.copyId).asOptionT
      } yield copy.offeredBy

      maybeCopyOwnerId.value.map {
        case Some(copyOwnerId) if copyOwnerId == userId =>
          Right(())
        case _ =>
          Left(NotTheRequestedCopyOwner(userId, copyRequestId))
      }
    }

  def byRequestIssuer[F[_]: Monad](
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

package com.github.sophiecollard.bookswap.services.transaction.copyrequests

import cats.Monad
import cats.implicits._
import com.github.sophiecollard.bookswap.authorization.error.AuthorizationError.{NotTheRequestIssuer, NotTheRequestedCopyOwner}
import com.github.sophiecollard.bookswap.authorization.{AuthorizationService, WithAuthorization}
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.CopyRequest
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.repositories.inventory.CopiesRepository
import com.github.sophiecollard.bookswap.repositories.transaction.CopyRequestsRepository
import com.github.sophiecollard.bookswap.syntax.OptionTSyntax.FOpToOptionT

object authorization {

  trait ByRequestIssuer
  trait ByCopyOwner

  type WithAuthorizationByRequestIssuer[R] = WithAuthorization[R, ByRequestIssuer]
  type WithAuthorizationByCopyOwner[R] = WithAuthorization[R, ByCopyOwner]

  type Input = (Id[User], Id[CopyRequest])

  def byCopyOwner[F[_]: Monad](
    copyRequestsRepository: CopyRequestsRepository[F],
    copiesRepository: CopiesRepository[F]
  ): AuthorizationService[F, Input, ByCopyOwner] =
    AuthorizationService.create { case (userId, copyRequestId) =>
      val maybeCopyOwnerId = for {
        copyRequest <- copyRequestsRepository.get(copyRequestId).asOptionT
        copy <- copiesRepository.get(copyRequest.copyId).asOptionT
      } yield copy.offeredBy

      maybeCopyOwnerId.value.map {
        case Some(copyOwnerId) if copyOwnerId == userId =>
          Right(())
        case _ =>
          Left(NotTheRequestedCopyOwner(userId, copyRequestId))
      }
    }

  def byRequestIssuer[F[_]: Monad](
    copyRequestsRepository: CopyRequestsRepository[F]
  ): AuthorizationService[F, Input, ByRequestIssuer] =
    AuthorizationService.create { case (userId, copyRequestId) =>
      copyRequestsRepository.get(copyRequestId).map {
        case Some(copyRequest) if copyRequest.requestedBy == userId =>
          Right(())
        case _ =>
          Left(NotTheRequestIssuer(userId, copyRequestId))
      }
    }

}

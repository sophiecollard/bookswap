package com.github.sophiecollard.bookswap.services.inventory.copy

import cats.Monad
import cats.implicits._
import com.github.sophiecollard.bookswap.domain.inventory.Copy
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.error.Error.AuthorizationError.NotTheCopyOwner
import com.github.sophiecollard.bookswap.repositories.inventory.CopyRepository
import com.github.sophiecollard.bookswap.services.authorization.{AuthorizationService, WithAuthorization}

object Authorization {

  trait ByCopyOwner

  type WithAuthorizationByCopyOwner[R] = WithAuthorization[R, ByCopyOwner]

  final case class AuthorizationInput(userId: Id[User], copyId: Id[Copy])

  def createCopyOwnerAuthorizationService[F[_]: Monad](
    copyRepository: CopyRepository[F]
  ): AuthorizationService[F, AuthorizationInput, ByCopyOwner] =
    AuthorizationService.create[F, AuthorizationInput, ByCopyOwner] { case AuthorizationInput(userId, copyId) =>
      copyRepository.get(copyId).map {
        case Some(copy) if copy.offeredBy == userId =>
          Right(())
        case _ =>
          Left(NotTheCopyOwner(userId, copyId))
      }
    }

}

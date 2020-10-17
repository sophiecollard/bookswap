package com.github.sophiecollard.bookswap.services.inventory.copy

import cats.Monad
import cats.implicits._
import com.github.sophiecollard.bookswap.authorization.error.AuthorizationError.NotTheCopyOwner
import com.github.sophiecollard.bookswap.authorization.{AuthorizationService, WithAuthorization}
import com.github.sophiecollard.bookswap.domain.inventory.Copy
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.repositories.inventory.CopiesRepository

object authorization {

  trait ByCopyOwner

  type WithAuthorizationByCopyOwner[R] = WithAuthorization[R, ByCopyOwner]

  type Input = (Id[User], Id[Copy])

  def byCopyOwner[F[_]: Monad](
    copiesRepository: CopiesRepository[F]
  ): AuthorizationService[F, Input, ByCopyOwner] =
    AuthorizationService.create { case (userId, copyId) =>
      copiesRepository.get(copyId).map {
        case Some(copy) if copy.offeredBy == userId =>
          Right(())
        case _ =>
          Left(NotTheCopyOwner(userId, copyId))
      }
    }

}

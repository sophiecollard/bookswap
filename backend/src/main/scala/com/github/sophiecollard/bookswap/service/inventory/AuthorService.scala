package com.github.sophiecollard.bookswap.service.inventory

import cats.{Applicative, Monad}
import cats.implicits._
import com.github.sophiecollard.bookswap.domain.inventory.Author
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.domain.user.UserStatus.Admin
import com.github.sophiecollard.bookswap.error.Error.NotAnAdminUser
import com.github.sophiecollard.bookswap.repositories.{AuthorRepository, UserRepository}
import com.github.sophiecollard.bookswap.service.authorization._

trait AuthorService[F[_]] {

  def delete(id: Id[Author])(userId: Id[User]): F[WithAuthorization[Unit]]

  def get(id: Id[Author]): F[Option[Author]]

}

object AuthorService {

  def createAuthorizationService[F[_]: Monad](
    userRepository: UserRepository[F]
  ): AuthorizationService[F, Id[User]] =
    AuthorizationService.create[F, Id[User]] { userId =>
      userRepository.get(userId).map {
        case Some(User(_, _, Admin)) =>
          Right(())
        case _ =>
          Left(NotAnAdminUser(userId))
      }
    }

  def create[F[_]: Applicative](
    authorizationService: AuthorizationService[F, Id[User]],
    repository: AuthorRepository[F]
  ): AuthorService[F] =
    new AuthorService[F] {
      override def delete(id: Id[Author])(userId: Id[User]): F[WithAuthorization[Unit]] =
        authorizationService.authorize(userId) {
          repository.delete(id)
        }

      override def get(id: Id[Author]): F[Option[Author]] =
        repository.get(id)
    }

}

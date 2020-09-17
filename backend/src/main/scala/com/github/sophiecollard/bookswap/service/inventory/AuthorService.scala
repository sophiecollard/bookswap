package com.github.sophiecollard.bookswap.service.inventory

import cats.{Applicative, Monad}
import cats.implicits._
import com.github.sophiecollard.bookswap.domain.inventory.Author
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.domain.user.UserStatus.Admin
import com.github.sophiecollard.bookswap.error.Error.NotAnAdmin
import com.github.sophiecollard.bookswap.repositories.{AuthorRepository, UserRepository}
import com.github.sophiecollard.bookswap.service.authorization._

trait AuthorService[F[_]] {

  import AuthorService.WithAuthorizationByAdminStatus

  def delete(id: Id[Author])(userId: Id[User]): F[WithAuthorizationByAdminStatus[Unit]]

  def get(id: Id[Author]): F[Option[Author]]

}

object AuthorService {

  def create[F[_]: Applicative](
    authorizationService: AuthorizationService[F, Id[User], ByAdminStatus],
    repository: AuthorRepository[F]
  ): AuthorService[F] =
    new AuthorService[F] {
      override def delete(id: Id[Author])(userId: Id[User]): F[WithAuthorizationByAdminStatus[Unit]] =
        authorizationService.authorize(userId) {
          repository.delete(id)
        }

      override def get(id: Id[Author]): F[Option[Author]] =
        repository.get(id)
    }

  trait ByAdminStatus

  type WithAuthorizationByAdminStatus[R] = WithAuthorization[R, ByAdminStatus]

  def createAuthorizationService[F[_]: Monad](
    userRepository: UserRepository[F]
  ): AuthorizationService[F, Id[User], ByAdminStatus] =
    AuthorizationService.create[F, Id[User], ByAdminStatus] { userId =>
      userRepository.get(userId).map {
        case Some(User(_, _, Admin)) =>
          Right(())
        case _ =>
          Left(NotAnAdmin(userId))
      }
    }

}

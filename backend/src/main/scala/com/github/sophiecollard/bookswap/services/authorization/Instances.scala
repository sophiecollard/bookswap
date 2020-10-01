package com.github.sophiecollard.bookswap.services.authorization

import cats.Monad
import cats.implicits._
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.domain.user.UserStatus.{Active, Admin}
import com.github.sophiecollard.bookswap.error.Error.AuthorizationError.{NotAnActiveUser, NotAnAdmin}
import com.github.sophiecollard.bookswap.repositories.user.UserRepository

object Instances {

  trait ByActiveStatus

  type WithAuthorizationByActiveStatus[R] = WithAuthorization[R, ByActiveStatus]

  def byActiveStatus[F[_]: Monad](
    userRepository: UserRepository[F]
  ): AuthorizationService[F, Id[User], ByActiveStatus] =
    AuthorizationService.create[F, Id[User], ByActiveStatus] { userId =>
      userRepository.get(userId).map {
        case Some(User(_, _, Active) | User(_, _, Admin)) =>
          Right(())
        case _ =>
          Left(NotAnActiveUser(userId))
      }
    }

  trait ByAdminStatus

  type WithAuthorizationByAdminStatus[R] = WithAuthorization[R, ByAdminStatus]

  def byAdminStatus[F[_]: Monad](
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

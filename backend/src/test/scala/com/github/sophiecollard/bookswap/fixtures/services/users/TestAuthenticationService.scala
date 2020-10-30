package com.github.sophiecollard.bookswap.fixtures.services.users

import cats.Applicative
import cats.implicits._
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.{Email, Password, User}
import com.github.sophiecollard.bookswap.services.users.AuthenticationService

object TestAuthenticationService {

  def create[F[_]: Applicative](userId: Id[User]): AuthenticationService[F] =
    new AuthenticationService[F] {
      override def authenticate(email: Email, password: Password): F[Option[Id[User]]] =
        userId.some.pure[F]
    }

}

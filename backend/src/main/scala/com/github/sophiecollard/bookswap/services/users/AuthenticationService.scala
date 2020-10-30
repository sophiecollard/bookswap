package com.github.sophiecollard.bookswap.services.users

import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.{Email, Password, User}

trait AuthenticationService[F[_]] {

  def authenticate(email: Email, password: Password): F[Option[Id[User]]]

}

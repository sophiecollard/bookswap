package com.github.sophiecollard.bookswap.repositories

import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User

trait UserRepository[F[_]] {

  def get(id: Id[User]): F[Option[User]]

}

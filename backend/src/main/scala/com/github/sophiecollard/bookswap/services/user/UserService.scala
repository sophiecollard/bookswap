package com.github.sophiecollard.bookswap.services.user

import com.github.sophiecollard.bookswap.domain.shared.{Id, Name}
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.error.Error.ServiceErrorOr
import com.github.sophiecollard.bookswap.services.authorization.Instances.WithAuthorizationBySelf

trait UserService[F[_]] {

  /** Fetches a User */
  def get(id: Id[User]): F[ServiceErrorOr[User]]

  /** Creates a new User */
  def create(name: Name[User]): F[ServiceErrorOr[User]]

  /** Invoked by a User to delete itself */
  def delete(id: Id[User])(userId: Id[User]): F[WithAuthorizationBySelf[ServiceErrorOr[Unit]]]

}

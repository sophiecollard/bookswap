package com.github.sophiecollard.bookswap.repositories.user

import cats.data.NonEmptyList
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User

trait ThreadRepository[F[_]] {

  def getParticipants(id: Id[Thread]): F[NonEmptyList[Id[User]]]

}

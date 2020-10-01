package com.github.sophiecollard.bookswap.fixtures.repositories.user

import cats.{Id => CatsId}
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.repositories.user.UserRepository

class TestUserRepository extends UserRepository[CatsId] {

  def create(user: User): CatsId[Boolean] =
    store.get(user.id) match {
      case Some(_) =>
        false
      case None =>
        store += ((user.id, user))
        true
    }

  def delete(id: Id[User]): CatsId[Boolean] = {
    store.get(id) match {
      case Some(_) =>
        store = store removed id
        true
      case None =>
        false
    }
  }

  def get(id: Id[User]): CatsId[Option[User]] =
    store.get(id)

  private var store: Map[Id[User], User] =
    Map.empty

}

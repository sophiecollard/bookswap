package com.github.sophiecollard.bookswap.fixtures.repositories.user

import cats.{Id => CatsId}
import com.github.sophiecollard.bookswap.domain.shared.{Id, Name}
import com.github.sophiecollard.bookswap.domain.user.{User, UserStatus}
import com.github.sophiecollard.bookswap.repositories.user.UserRepository

class TestUserRepository extends UserRepository[CatsId] {

  override def create(user: User): CatsId[Boolean] =
    store.get(user.id) match {
      case Some(_) =>
        false
      case None =>
        store += ((user.id, user))
        true
    }

  override def updateStatus(id: Id[User], status: UserStatus): CatsId[Boolean] =
    store.get(id) match {
      case Some(user) =>
        store += ((id, user.copy(status = status)))
        true
      case None =>
        false
    }

  override def delete(id: Id[User]): CatsId[Boolean] = {
    store.get(id) match {
      case Some(_) =>
        store -= id
        true
      case None =>
        false
    }
  }

  override def get(id: Id[User]): CatsId[Option[User]] =
    store.get(id)

  override def getByName(name: Name[User]): CatsId[Option[User]] =
    store.values.find(_.name == name)

  private var store: Map[Id[User], User] =
    Map.empty

}

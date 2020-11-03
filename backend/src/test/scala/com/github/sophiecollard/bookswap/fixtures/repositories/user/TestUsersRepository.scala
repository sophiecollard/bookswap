package com.github.sophiecollard.bookswap.fixtures.repositories.user

import cats.Applicative
import cats.implicits._
import com.github.sophiecollard.bookswap.domain.shared.{Id, Name}
import com.github.sophiecollard.bookswap.domain.user.{User, UserStatus}
import com.github.sophiecollard.bookswap.repositories.users.UsersRepository

object TestUsersRepository {

  def create[F[_]: Applicative]: UsersRepository[F] =
    new UsersRepository[F] {
      override def create(user: User): F[Boolean] =
        store.get(user.id) match {
          case Some(_) =>
            false.pure[F]
          case None =>
            store += ((user.id, user))
            true.pure[F]
        }

      override def updateStatus(id: Id[User], status: UserStatus): F[Boolean] =
        store.get(id) match {
          case Some(user) =>
            store += ((id, user.copy(status = status)))
            true.pure[F]
          case None =>
            false.pure[F]
        }

      override def delete(id: Id[User]): F[Boolean] = {
        store.get(id) match {
          case Some(_) =>
            store -= id
            true.pure[F]
          case None =>
            false.pure[F]
        }
      }

      override def get(id: Id[User]): F[Option[User]] =
        store.get(id).pure[F]

      override def getByName(name: Name[User]): F[Option[User]] =
        store.values.find(_.name == name).pure[F]

      private var store: Map[Id[User], User] =
        Map.empty
    }

}

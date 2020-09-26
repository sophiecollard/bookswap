package com.github.sophiecollard.bookswap.repositories.user

import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User
import doobie.{ConnectionIO, Query0, Update0}
import doobie.implicits._

trait UserRepository[F[_]] {

  def get(id: Id[User]): F[Option[User]]

  def delete(id: Id[User]): F[Unit]

}

object UserRepository {

  def create: UserRepository[ConnectionIO] = new UserRepository[ConnectionIO] {
    override def get(id: Id[User]): ConnectionIO[Option[User]] =
      getQuery(id)
        .option

    override def delete(id: Id[User]): ConnectionIO[Unit] =
      deleteUpdate(id)
        .run
        .map(_ => ())
  }

  def getQuery(id: Id[User]): Query0[User] =
    sql"""
         |SELECT id, name, status
         |FROM users
         |WHERE id = $id
       """.stripMargin.query[User]

  def deleteUpdate(id: Id[User]): Update0 =
    sql"""
         |DELETE
         |FROM users
         |WHERE id = $id
       """.stripMargin.update

}

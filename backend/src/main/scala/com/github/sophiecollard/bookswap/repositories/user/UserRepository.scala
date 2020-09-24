package com.github.sophiecollard.bookswap.repositories.user

import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User
import doobie.{ConnectionIO, Query0}
import doobie.implicits._

trait UserRepository[F[_]] {

  def get(id: Id[User]): F[Option[User]]

}

object UserRepository {

  def create: UserRepository[ConnectionIO] = new UserRepository[ConnectionIO] {
    override def get(id: Id[User]): ConnectionIO[Option[User]] =
      getQuery(id)
        .option
  }

  def getQuery(id: Id[User]): Query0[User] =
    sql"""
         |SELECT id, name, status
         |FROM users
         |WHERE id = $id
       """.stripMargin.query[User]

}

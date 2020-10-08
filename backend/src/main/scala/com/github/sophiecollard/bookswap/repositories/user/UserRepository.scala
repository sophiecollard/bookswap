package com.github.sophiecollard.bookswap.repositories.user

import com.github.sophiecollard.bookswap.domain.shared.{Id, Name}
import com.github.sophiecollard.bookswap.domain.user.{User, UserStatus}
import doobie.{ConnectionIO, Query0, Update, Update0}
import doobie.implicits._

trait UserRepository[F[_]] {

  def create(user: User): F[Boolean]

  def updateStatus(id: Id[User], status: UserStatus): F[Boolean]

  def delete(id: Id[User]): F[Boolean]

  def get(id: Id[User]): F[Option[User]]

  def getByName(name: Name[User]): F[Option[User]]

}

object UserRepository {

  def create: UserRepository[ConnectionIO] = new UserRepository[ConnectionIO] {
    override def create(user: User): ConnectionIO[Boolean] =
      createUpdate
        .run(user)
        .map(_ == 1)

    override def updateStatus(id: Id[User], status: UserStatus): ConnectionIO[Boolean] =
      updateStatusUpdate(id, status)
        .run
        .map(_ == 1)

    override def delete(id: Id[User]): ConnectionIO[Boolean] =
      deleteUpdate(id)
        .run
        .map(_ == 1)

    override def get(id: Id[User]): ConnectionIO[Option[User]] =
      getQuery(id)
        .option

    override def getByName(name: Name[User]): ConnectionIO[Option[User]] =
      getByNameQuery(name)
        .option
  }

  def createUpdate: Update[User] =
    Update(
      s"""
         |INSERT INTO users (id, name, status)
         |VALUES (?, ?, ?)
         |ON CONFLICT id DO NOTHING
         |""".stripMargin
    )

  def updateStatusUpdate(id: Id[User], status: UserStatus): Update0 =
    sql"""
         |UPDATE users
         |SET status = $status
         |WHERE id = $id
       """.stripMargin.update

  def deleteUpdate(id: Id[User]): Update0 =
    sql"""
         |DELETE
         |FROM users
         |WHERE id = $id
       """.stripMargin.update

  def getQuery(id: Id[User]): Query0[User] =
    sql"""
         |SELECT id, name, status
         |FROM users
         |WHERE id = $id
       """.stripMargin.query[User]

  def getByNameQuery(name: Name[User]): Query0[User] =
    sql"""
         |SELECT id, name, status
         |FROM users
         |WHERE name = $name
       """.stripMargin.query[User]

}

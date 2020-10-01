package com.github.sophiecollard.bookswap.repositories.inventory

import com.github.sophiecollard.bookswap.domain.inventory.Author
import com.github.sophiecollard.bookswap.domain.shared.Id
import doobie.{ConnectionIO, Query0, Update0}
import doobie.implicits._

trait AuthorRepository[F[_]] {

  def get(id: Id[Author]): F[Option[Author]]

  def delete(id: Id[Author]): F[Boolean]

}

object AuthorRepository {

  def create: AuthorRepository[ConnectionIO] = new AuthorRepository[ConnectionIO] {
    override def get(id: Id[Author]): ConnectionIO[Option[Author]] =
      getQuery(id)
        .option

    override def delete(id: Id[Author]): ConnectionIO[Boolean] =
      deleteUpdate(id)
        .run
        .map(_ == 1)
  }

  def getQuery(id: Id[Author]): Query0[Author] =
    sql"""
         |SELECT id, name
         |FROM authors
         |WHERE id = $id
       """.stripMargin.query[Author]

  def deleteUpdate(id: Id[Author]): Update0 =
    sql"""
         |DELETE
         |FROM authors
         |WHERE id = $id
       """.stripMargin.update

}

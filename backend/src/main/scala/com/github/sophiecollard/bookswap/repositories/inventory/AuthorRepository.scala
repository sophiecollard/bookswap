package com.github.sophiecollard.bookswap.repositories.inventory

import com.github.sophiecollard.bookswap.domain.inventory.Author
import com.github.sophiecollard.bookswap.domain.shared.Id
import doobie.{ConnectionIO, Query0, Update0, Update}
import doobie.implicits._

trait AuthorRepository[F[_]] {

  /** Returns the specified Author */
  def get(id: Id[Author]): F[Option[Author]]

  /** Creates a new Author */
  def create(author: Author): F[Boolean]

  /** Deletes the specified Author */
  def delete(id: Id[Author]): F[Boolean]

}

object AuthorRepository {

  def create: AuthorRepository[ConnectionIO] = new AuthorRepository[ConnectionIO] {
    override def get(id: Id[Author]): ConnectionIO[Option[Author]] =
      getQuery(id)
        .option

    override def create(author: Author): ConnectionIO[Boolean] =
      createUpdate
        .run(author)
        .map(_ == 1)

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

  def createUpdate: Update[Author] =
    Update(
      s"""
         |INSERT INTO authors (id, name)
         |VALUES (?, ?)
         |ON CONFLICT id DO NOTHING
         |""".stripMargin
    )

  def deleteUpdate(id: Id[Author]): Update0 =
    sql"""
         |DELETE
         |FROM authors
         |WHERE id = $id
       """.stripMargin.update

}

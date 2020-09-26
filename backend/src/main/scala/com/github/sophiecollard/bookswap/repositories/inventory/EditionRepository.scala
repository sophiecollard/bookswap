package com.github.sophiecollard.bookswap.repositories.inventory

import com.github.sophiecollard.bookswap.domain.inventory.{Edition, ISBN}
import com.github.sophiecollard.bookswap.implicits.All._
import doobie.{ConnectionIO, Query0, Update0}
import doobie.implicits._
import doobie.implicits.javatime._

trait EditionRepository[F[_]] {

  def get(isbn: ISBN): F[Option[Edition]]

  def delete(isbn: ISBN): F[Unit]

}

object EditionRepository {

  def create: EditionRepository[ConnectionIO] = new EditionRepository[ConnectionIO] {
    override def get(isbn: ISBN): ConnectionIO[Option[Edition]] =
      getQuery(isbn)
        .option

    override def delete(isbn: ISBN): ConnectionIO[Unit] =
      deleteUpdate(isbn)
        .run
        .map(_ => ())
  }

  def getQuery(isbn: ISBN): Query0[Edition] =
    sql"""
         |SELECT isbn, title, author_ids, publisher_id, publication_date
         |FROM editions
         |WHERE isbn = $isbn
       """.stripMargin.query[Edition]

  def deleteUpdate(isbn: ISBN): Update0 =
    sql"""
         |DELETE
         |FROM editions
         |WHERE isbn = $isbn
       """.stripMargin.update

}

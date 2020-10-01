package com.github.sophiecollard.bookswap.repositories.inventory

import com.github.sophiecollard.bookswap.domain.inventory.{Edition, EditionDetails, ISBN}
import com.github.sophiecollard.bookswap.implicits.All._
import doobie.{ConnectionIO, Query0, Update0, Update}
import doobie.implicits._
import doobie.implicits.javatime._

trait EditionRepository[F[_]] {

  /** Creates a new Edition */
  def create(edition: Edition): F[Unit]

  /** Updates the details of the specified Edition */
  def update(isbn: ISBN, details: EditionDetails): F[Unit]

  /** Deletes the specified Edition */
  def delete(isbn: ISBN): F[Unit]

  /** Returns the specified Edition */
  def get(isbn: ISBN): F[Option[Edition]]

}

object EditionRepository {

  def create: EditionRepository[ConnectionIO] = new EditionRepository[ConnectionIO] {
    override def create(edition: Edition): ConnectionIO[Unit] =
      createUpdate
        .run(edition)
        .map(_ => ())

    override def update(isbn: ISBN, details: EditionDetails): ConnectionIO[Unit] =
      updateUpdate(isbn, details)
        .run
        .map(_ => ())

    override def delete(isbn: ISBN): ConnectionIO[Unit] =
      deleteUpdate(isbn)
        .run
        .map(_ => ())

    override def get(isbn: ISBN): ConnectionIO[Option[Edition]] =
      getQuery(isbn)
        .option
  }

  val createUpdate: Update[Edition] =
    Update[Edition](
      s"""
         |INSERT INTO editions (isbn, title, author_ids, publisher_id, publication_date)
         |VALUES (?, ?, ?, ?, ?)
         |ON CONFLICT isbn DO NOTHING
       """.stripMargin
    )

  def updateUpdate(isbn: ISBN, details: EditionDetails): Update0 =
    sql"""
         |UPDATE editions
         |SET title = ${details.title},
         |author_ids = ${details.authorIds},
         |publisher_id = ${details.publisherId},
         |publication_date = ${details.publicationDate}
         |WHERE isbn = $isbn
       """.stripMargin.update

  def deleteUpdate(isbn: ISBN): Update0 =
    sql"""
         |DELETE
         |FROM editions
         |WHERE isbn = $isbn
       """.stripMargin.update

  def getQuery(isbn: ISBN): Query0[Edition] =
    sql"""
         |SELECT isbn, title, author_ids, publisher_id, publication_date
         |FROM editions
         |WHERE isbn = $isbn
       """.stripMargin.query[Edition]

}

package com.github.sophiecollard.bookswap.repositories.inventory

import com.github.sophiecollard.bookswap.domain.inventory.{Edition, EditionDetails, ISBN}
import com.github.sophiecollard.bookswap.repositories.instances._
import doobie.implicits._
import doobie.implicits.javatime._
import doobie.{ConnectionIO, Query0, Update, Update0}

trait EditionRepository[F[_]] {

  /** Creates a new Edition */
  def create(edition: Edition): F[Boolean]

  /** Updates the details of the specified Edition */
  def update(isbn: ISBN, details: EditionDetails): F[Boolean]

  /** Deletes the specified Edition */
  def delete(isbn: ISBN): F[Boolean]

  /** Returns the specified Edition */
  def get(isbn: ISBN): F[Option[Edition]]

}

object EditionRepository {

  def create: EditionRepository[ConnectionIO] = new EditionRepository[ConnectionIO] {
    override def create(edition: Edition): ConnectionIO[Boolean] =
      createUpdate
        .run(edition)
        .map(_ == 1)

    override def update(isbn: ISBN, details: EditionDetails): ConnectionIO[Boolean] =
      updateUpdate(isbn, details)
        .run
        .map(_ == 1)

    override def delete(isbn: ISBN): ConnectionIO[Boolean] =
      deleteUpdate(isbn)
        .run
        .map(_ == 1)

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

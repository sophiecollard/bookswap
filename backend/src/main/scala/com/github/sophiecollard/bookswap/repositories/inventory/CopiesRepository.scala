package com.github.sophiecollard.bookswap.repositories.inventory

import com.github.sophiecollard.bookswap.domain.inventory.{Condition, Copy, CopyPagination, CopyStatus, ISBN}
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User
import doobie.implicits._
import doobie.implicits.javatime._
import doobie.{ConnectionIO, Query0, Update, Update0}

trait CopiesRepository[F[_]] {

  /** Creates a new Copy */
  def create(copy: Copy): F[Boolean]

  /** Updates the condition of the specified Copy */
  def updateCondition(id: Id[Copy], condition: Condition): F[Boolean]

  /** Updates the status of the specified Copy */
  def updateStatus(id: Id[Copy], status: CopyStatus): F[Boolean]

  /** Returns the specified Copy */
  def get(id: Id[Copy]): F[Option[Copy]]

  /** Returns a list of Copies available or reserved for the specified ISBN */
  def listForEdition(isbn: ISBN, pagination: CopyPagination): F[List[Copy]]

  /** Returns a list of Copies offered by the specified User */
  def listForOwner(offeredBy: Id[User], pagination: CopyPagination): F[List[Copy]]

}

object CopiesRepository {

  def create: CopiesRepository[ConnectionIO] = new CopiesRepository[ConnectionIO] {
    override def create(copy: Copy): ConnectionIO[Boolean] =
      createUpdate
        .run(copy)
        .map(_ == 1)

    override def updateCondition(id: Id[Copy], condition: Condition): ConnectionIO[Boolean] =
      updateConditionUpdate(id, condition)
        .run
        .map(_ == 1)

    override def updateStatus(id: Id[Copy], status: CopyStatus): ConnectionIO[Boolean] =
      updateStatusUpdate(id, status)
        .run
        .map(_ == 1)

    override def get(id: Id[Copy]): ConnectionIO[Option[Copy]] =
      getQuery(id)
        .option

    override def listForEdition(isbn: ISBN, pagination: CopyPagination): ConnectionIO[List[Copy]] =
      listForEditionQuery(isbn, pagination)
        .to[List]

    override def listForOwner(offeredBy: Id[User], pagination: CopyPagination): ConnectionIO[List[Copy]] =
      listForOwnerQuery(offeredBy, pagination)
        .to[List]
  }

  val createUpdate: Update[Copy] =
    Update[Copy](
      s"""
         |INSERT INTO copies (id, isbn, offered_by, offered_on, condition, status)
         |VALUES (?, ?, ?, ?, ?, ?)
         |ON CONFLICT id DO NOTHING
       """.stripMargin
    )

  def updateConditionUpdate(id: Id[Copy], condition: Condition): Update0 =
    sql"""
         |UPDATE copies
         |SET condition = $condition
         |WHERE id = $id
       """.stripMargin.update

  def updateStatusUpdate(id: Id[Copy], status: CopyStatus): Update0 =
    sql"""
         |UPDATE copies
         |SET status = $status
         |WHERE id = $id
       """.stripMargin.update

  def getQuery(id: Id[Copy]): Query0[Copy] =
    sql"""
         |SELECT id, isbn, offered_by, offered_on, condition, status
         |FROM copies
         |WHERE id = $id
       """.stripMargin.query[Copy]

  def listForEditionQuery(isbn: ISBN, pagination: CopyPagination): Query0[Copy] =
    sql"""
         |SELECT id, isbn, offered_by, offered_on, condition, status
         |FROM copies
         |WHERE isbn = $isbn
         |AND status != 'withdrawn'
         |AND status != 'swapped'
         |AND offered_on <= ${pagination.offeredOnOrBefore}
         |ORDER BY offered_on DESC
         |LIMIT ${pagination.pageSize.value}
       """.stripMargin.query[Copy]

  def listForOwnerQuery(offeredBy: Id[User], pagination: CopyPagination): Query0[Copy] =
    sql"""
         |SELECT id, isbn, offered_by, offered_on, condition, status
         |FROM copies
         |WHERE offered_by = $offeredBy
         |AND offered_on <= ${pagination.offeredOnOrBefore}
         |ORDER BY offered_on DESC
         |LIMIT ${pagination.pageSize.value}
       """.stripMargin.query[Copy]

}

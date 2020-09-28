package com.github.sophiecollard.bookswap.repositories.inventory

import doobie.implicits._
import doobie.implicits.javatime._
import com.github.sophiecollard.bookswap.domain.inventory.{Condition, Copy, CopyStatus}
import com.github.sophiecollard.bookswap.domain.shared.Id
import doobie.{ConnectionIO, Query0, Update0}

trait CopyRepository[F[_]] {

  def create(copy: Copy): F[Unit]

  def updateCondition(id: Id[Copy], condition: Condition): F[Unit]

  def updateStatus(id: Id[Copy], status: CopyStatus): F[Unit]

  def get(id: Id[Copy]): F[Option[Copy]]

}

object CopyRepository {

  def create: CopyRepository[ConnectionIO] = new CopyRepository[ConnectionIO] {
    override def create(copy: Copy): ConnectionIO[Unit] =
      insertCopyUpdate(copy)
        .run
        .map(_ => ())

    override def updateCondition(id: Id[Copy], condition: Condition): ConnectionIO[Unit] =
      updateConditionUpdate(id, condition)
        .run
        .map(_ => ())

    override def updateStatus(id: Id[Copy], status: CopyStatus): ConnectionIO[Unit] =
      updateStatusUpdate(id, status)
        .run
        .map(_ => ())

    override def get(id: Id[Copy]): ConnectionIO[Option[Copy]] =
      getQuery(id)
        .option
  }

  def insertCopyUpdate(copy: Copy): Update0 =
    sql"""
         |INSERT INTO copies (id, isbn, offered_by, offered_on, condition, status)
         |VALUES (${copy.id}, ${copy.isbn}, ${copy.offeredBy}, ${copy.offeredOn}, ${copy.condition}, ${copy.status})
         |ON CONFLICT id DO NOTHING
       """.stripMargin.update

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

}

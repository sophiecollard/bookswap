package com.github.sophiecollard.bookswap.repositories.inventory

import doobie.implicits._
import doobie.implicits.javatime._
import com.github.sophiecollard.bookswap.domain.inventory.{Copy, CopyStatus}
import com.github.sophiecollard.bookswap.domain.shared.Id
import doobie.{ConnectionIO, Query0, Update0}

trait CopyRepository[F[_]] {

  def create(copy: Copy): F[Unit]

  def updateStatus(id: Id[Copy], newStatus: CopyStatus): F[Unit]

  def get(id: Id[Copy]): F[Option[Copy]]

}

object CopyRepository {

  def create: CopyRepository[ConnectionIO] = new CopyRepository[ConnectionIO] {
    override def create(copy: Copy): ConnectionIO[Unit] =
      insertCopyUpdate(copy)
        .run
        .map(_ => ())

    override def updateStatus(id: Id[Copy], newStatus: CopyStatus): ConnectionIO[Unit] =
      updateStatusUpdate(id, newStatus)
        .run
        .map(_ => ())

    override def get(id: Id[Copy]): ConnectionIO[Option[Copy]] =
      getQuery(id)
        .option
  }

  def insertCopyUpdate(copy: Copy): Update0 =
    sql"""
         |INSERT INTO copies (id, edition, offered_by, offered_on, condition, status)
         |VALUES (${copy.id}, ${copy.edition}, ${copy.offeredBy}, ${copy.offeredOn}, ${copy.condition}, ${copy.status})
         |ON CONFLICT id DO NOTHING
       """.stripMargin.update

  def updateStatusUpdate(id: Id[Copy], newStatus: CopyStatus): Update0 =
    sql"""
         |UPDATE copies
         |SET status = $newStatus
         |WHERE id = $id
       """.stripMargin.update

  def getQuery(id: Id[Copy]): Query0[Copy] =
    sql"""
         |SELECT id, edition, offered_by, offered_on, condition, status
         |FROM copies
         |WHERE id = $id
       """.stripMargin.query[Copy]

}

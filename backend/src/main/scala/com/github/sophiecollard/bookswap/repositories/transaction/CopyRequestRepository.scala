package com.github.sophiecollard.bookswap.repositories.transaction

import com.github.sophiecollard.bookswap.domain.inventory.Copy
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.{CopyRequest, RequestStatus}
import doobie.{ConnectionIO, Query0, Update}
import doobie.implicits._
import doobie.implicits.javatime._

trait CopyRequestRepository[F[_]] {

  /**
    * Creates a new request
    */
  def create(copyRequest: CopyRequest): F[Unit]

  /**
    * Updates the status of the specified request
    */
  def updateStatus(id: Id[CopyRequest], newStatus: RequestStatus): F[Unit]

  /**
    * Updates the statuses of all open requests (i.e. pending or on the waiting list) for the specified copy
    */
  def updateOpenRequestsStatuses(copyId: Id[Copy], newStatus: RequestStatus): F[Unit]

  /**
    * Returns the specified request (if any)
    */
  def get(id: Id[CopyRequest]): F[Option[CopyRequest]]

  /**
    * Returns the first request added to the waiting list for the specified copy
    */
  def findFirstOnWaitingList(copyId: Id[Copy]): F[Option[CopyRequest]]

}

object CopyRequestRepository {

  def create: CopyRequestRepository[ConnectionIO] = new CopyRequestRepository[ConnectionIO] {
    override def create(copyRequest: CopyRequest): ConnectionIO[Unit] =
      createUpdate
        .run(copyRequest)
        .map(_ => ())

    override def updateStatus(id: Id[CopyRequest], newStatus: RequestStatus): ConnectionIO[Unit] =
      updateStatusUpdate(id)
        .run(newStatus)
        .map(_ => ())

    override def updateOpenRequestsStatuses(copyId: Id[Copy], newStatus: RequestStatus): ConnectionIO[Unit] =
      updateOpenRequestsStatusesUpdate(copyId)
        .run(newStatus)
        .map(_ => ())

    override def get(id: Id[CopyRequest]): ConnectionIO[Option[CopyRequest]] =
      getQuery(id)
        .option

    override def findFirstOnWaitingList(copyId: Id[Copy]): ConnectionIO[Option[CopyRequest]] =
      findFirstOnWaitingListQuery(copyId)
        .option
  }

  val createUpdate: Update[CopyRequest] =
    Update[CopyRequest](
      s"""
         |INSERT INTO copy_requests (id, copy_id, requested_by, requested_on, status_name, status_timestamp)
         |VALUES (?, ?, ?, ?, ?, ?)
         |ON CONFLICT id DO NOTHING
       """.stripMargin
    )

  def updateStatusUpdate(id: Id[CopyRequest]): Update[RequestStatus] =
    Update[RequestStatus](
      s"""
         |UPDATE copy_requests
         |SET (status_name, status_timestamp) = (?, ?)
         |WHERE id = $id
       """.stripMargin
    )

  def updateOpenRequestsStatusesUpdate(copyId: Id[Copy]): Update[RequestStatus] =
    Update[RequestStatus](
      s"""
         |UPDATE copy_requests
         |SET (status_name, status_timestamp) = (?, ?)
         |WHERE copy_id = $copyId
         |AND status_name = 'pending'
         |OR status_name = 'on_waiting_list'
       """.stripMargin
    )

  def getQuery(id: Id[CopyRequest]): Query0[CopyRequest] =
    sql"""
         |SELECT id, copy_id, requested_by, requested_on, status_name, status_timestamp
         |FROM copy_requests
         |WHERE id = $id
       """.stripMargin.query[CopyRequest]

  def findFirstOnWaitingListQuery(copyId: Id[Copy]): Query0[CopyRequest] =
    sql"""
         |SELECT id, copy_id, requested_by, requested_on, status_name, status_timestamp
         |FROM copy_requests
         |WHERE copy_id = $copyId
         |AND status_name = 'on_waiting_list'
         |ORDER BY requested_on ASC
         |LIMIT 1
       """.stripMargin.query[CopyRequest]

}

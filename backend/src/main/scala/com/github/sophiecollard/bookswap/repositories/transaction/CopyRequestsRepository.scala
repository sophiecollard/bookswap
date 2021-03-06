package com.github.sophiecollard.bookswap.repositories.transaction

import com.github.sophiecollard.bookswap.domain.inventory.Copy
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.{CopyRequest, CopyRequestPagination, RequestStatus}
import com.github.sophiecollard.bookswap.domain.user.User
import doobie.implicits._
import doobie.implicits.javatime._
import doobie.{ConnectionIO, Query0, Update}

trait CopyRequestsRepository[F[_]] {

  /** Creates a new CopyRequest */
  def create(copyRequest: CopyRequest): F[Boolean]

  /** Updates the status of the specified CopyRequest */
  def updateStatus(id: Id[CopyRequest], newStatus: RequestStatus): F[Boolean]

  /** Updates the statuses of all pending CopyRequests for the specified Copy */
  def updatePendingRequestsStatuses(copyId: Id[Copy], newStatus: RequestStatus): F[Unit]

  /** Updates the status of any accepted CopyRequest for the specified Copy */
  def updateAcceptedRequestsStatuses(copyId: Id[Copy], newStatus: RequestStatus): F[Unit]

  /** Updates the statuses of all CopyRequests on the waiting list for the specified Copy */
  def updateWaitingListRequestsStatuses(copyId: Id[Copy], newStatus: RequestStatus): F[Unit]

  /** Returns the specified CopyRequest */
  def get(id: Id[CopyRequest]): F[Option[CopyRequest]]

  /** Returns the first CopyRequest added to the waiting list for the specified Copy */
  def findFirstOnWaitingList(copyId: Id[Copy]): F[Option[CopyRequest]]

  /** Returns a list of CopyRequests for the specified Copy */
  def listForCopy(copyId: Id[Copy], pagination: CopyRequestPagination): F[List[CopyRequest]]

  /** Returns a list of CopyRequests requested by the specified User */
  def listForRequester(requestedBy: Id[User], pagination: CopyRequestPagination): F[List[CopyRequest]]

}

object CopyRequestsRepository {

  def create: CopyRequestsRepository[ConnectionIO] = new CopyRequestsRepository[ConnectionIO] {
    override def create(copyRequest: CopyRequest): ConnectionIO[Boolean] =
      createUpdate
        .run(copyRequest)
        .map(_ == 1)

    override def updateStatus(id: Id[CopyRequest], newStatus: RequestStatus): ConnectionIO[Boolean] =
      updateStatusUpdate(id)
        .run(newStatus)
        .map(_ == 1)

    override def updatePendingRequestsStatuses(copyId: Id[Copy], newStatus: RequestStatus): ConnectionIO[Unit] =
      updatePendingRequestsStatusesUpdate(copyId)
        .run(newStatus)
        .map(_ => ())

    override def updateAcceptedRequestsStatuses(copyId: Id[Copy], newStatus: RequestStatus): ConnectionIO[Unit] =
      updateAcceptedRequestsStatusesUpdate(copyId)
        .run(newStatus)
        .map(_ => ())

    override def updateWaitingListRequestsStatuses(copyId: Id[Copy], newStatus: RequestStatus): ConnectionIO[Unit] =
      updateWaitingListRequestsStatusesUpdate(copyId)
        .run(newStatus)
        .map(_ => ())

    override def get(id: Id[CopyRequest]): ConnectionIO[Option[CopyRequest]] =
      getQuery(id)
        .option

    override def findFirstOnWaitingList(copyId: Id[Copy]): ConnectionIO[Option[CopyRequest]] =
      findFirstOnWaitingListQuery(copyId)
        .option

    override def listForCopy(copyId: Id[Copy], pagination: CopyRequestPagination): ConnectionIO[List[CopyRequest]] =
      listForCopyQuery(copyId, pagination)
        .to[List]

    override def listForRequester(requestedBy: Id[User], pagination: CopyRequestPagination): ConnectionIO[List[CopyRequest]] =
      listForRequesterQuery(requestedBy, pagination)
        .to[List]
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

  def updatePendingRequestsStatusesUpdate(copyId: Id[Copy]): Update[RequestStatus] =
    Update[RequestStatus](
      s"""
         |UPDATE copy_requests
         |SET (status_name, status_timestamp) = (?, ?)
         |WHERE copy_id = $copyId
         |AND status_name = 'pending'
       """.stripMargin
    )

  def updateAcceptedRequestsStatusesUpdate(copyId: Id[Copy]): Update[RequestStatus] =
    Update[RequestStatus](
      s"""
         |UPDATE copy_requests
         |SET (status_name, status_timestamp) = (?, ?)
         |WHERE copy_id = $copyId
         |AND status_name = 'accepted'
       """.stripMargin
    )

  def updateWaitingListRequestsStatusesUpdate(copyId: Id[Copy]): Update[RequestStatus] =
    Update[RequestStatus](
      s"""
         |UPDATE copy_requests
         |SET (status_name, status_timestamp) = (?, ?)
         |WHERE copy_id = $copyId
         |AND status_name = 'on_waiting_list'
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

  def listForCopyQuery(copyId: Id[Copy], pagination: CopyRequestPagination): Query0[CopyRequest] =
    sql"""
         |SELECT id, copy_id, requested_by, requested_on, status_name, status_timestamp
         |FROM copy_requests
         |WHERE copy_id = $copyId
         |AND requested_on <= ${pagination.requestedOnOrBefore}
         |ORDER BY requested_on DESC
         |LIMIT ${pagination.pageSize.value}
       """.stripMargin.query[CopyRequest]

  def listForRequesterQuery(requestedBy: Id[User], pagination: CopyRequestPagination): Query0[CopyRequest] =
    sql"""
         |SELECT id, copy_id, requested_by, requested_on, status_name, status_timestamp
         |FROM copy_requests
         |WHERE requested_by = $requestedBy
         |AND requested_on <= ${pagination.requestedOnOrBefore}
         |ORDER BY requested_on DESC
         |LIMIT ${pagination.pageSize.value}
       """.stripMargin.query[CopyRequest]

}

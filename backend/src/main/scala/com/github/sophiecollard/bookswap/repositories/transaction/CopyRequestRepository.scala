package com.github.sophiecollard.bookswap.repositories.transaction

import com.github.sophiecollard.bookswap.domain.inventory.Copy
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.{CopyRequest, RequestStatus}

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

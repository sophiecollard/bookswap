package com.github.sophiecollard.bookswap.fixtures.repositories.transaction

import cats.{Id => CatsId}
import com.github.sophiecollard.bookswap.domain.inventory.Copy
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.RequestStatus.{OnWaitingList, Pending}
import com.github.sophiecollard.bookswap.domain.transaction.{CopyRequest, RequestStatus}
import com.github.sophiecollard.bookswap.repositories.transaction.CopyRequestRepository

class TestCopyRequestRepository extends CopyRequestRepository[CatsId] {

  def create(copyRequest: CopyRequest): CatsId[Unit] =
    store += ((copyRequest.id, copyRequest))

  def updateStatus(id: Id[CopyRequest], newStatus: RequestStatus): CatsId[Unit] =
    store.get(id) match {
      case Some(copyRequest) =>
        store += ((id, copyRequest.copy(status = newStatus)))
      case None =>
        ()
    }

  def updateOpenRequestsStatuses(copyId: Id[Copy], newStatus: RequestStatus): CatsId[Unit] =
    store = store.map { case (id, request) =>
      request.status match {
        case Pending | OnWaitingList(_) =>
          (id, request.copy(status = newStatus))
        case _ =>
          (id, request)
      }
    }

  def get(id: Id[CopyRequest]): CatsId[Option[CopyRequest]] =
    store.get(id)

  def findFirstOnWaitingList(copyId: Id[Copy]): CatsId[Option[CopyRequest]] =
    store
      .values
      .toList
      .filter(r => r.copyId == copyId && r.status.isOnWaitingList)
      .sortBy(_.requestedOn)
      .headOption

  private var store: Map[Id[CopyRequest], CopyRequest] =
    Map.empty

}

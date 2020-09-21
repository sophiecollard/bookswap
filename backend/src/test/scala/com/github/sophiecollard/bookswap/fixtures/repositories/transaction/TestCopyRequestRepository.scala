package com.github.sophiecollard.bookswap.fixtures.repositories.transaction

import cats.{Id => CatsId}
import com.github.sophiecollard.bookswap.domain.inventory.Copy
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.{CopyRequest, RequestStatus}
import com.github.sophiecollard.bookswap.repositories.transaction.CopyRequestRepository

object TestCopyRequestRepository extends CopyRequestRepository[CatsId] {

  def create(copyRequest: CopyRequest): CatsId[Unit] =
    store += ((copyRequest.id, copyRequest))

  def updateStatus(id: Id[CopyRequest], newStatus: RequestStatus): CatsId[Unit] =
    store.get(id) match {
      case Some(copyRequest) =>
        store += ((id, copyRequest.copy(status = newStatus)))
      case None =>
        ()
    }

  def get(id: Id[CopyRequest]): CatsId[Option[CopyRequest]] =
    store.get(id)

  def findFirstOnWaitingList(copyId: Id[Copy]): CatsId[Option[CopyRequest]] =
    store
      .values
      .toList
      .filter(_.copyId == copyId)
      .sortBy(_.requestedOn)
      .headOption

  private var store: Map[Id[CopyRequest], CopyRequest] =
    Map.empty

}

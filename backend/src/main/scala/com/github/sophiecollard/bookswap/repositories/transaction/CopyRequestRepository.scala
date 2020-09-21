package com.github.sophiecollard.bookswap.repositories.transaction

import com.github.sophiecollard.bookswap.domain.inventory.Copy
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.{CopyRequest, RequestStatus}

trait CopyRequestRepository[F[_]] {

  def create(copyRequest: CopyRequest): F[Unit]

  def updateStatus(id: Id[CopyRequest], newStatus: RequestStatus): F[Unit]

  def get(id: Id[CopyRequest]): F[Option[CopyRequest]]

  def findFirstOnWaitingList(copyId: Id[Copy]): F[Option[CopyRequest]]

}

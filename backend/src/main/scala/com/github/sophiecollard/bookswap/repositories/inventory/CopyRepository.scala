package com.github.sophiecollard.bookswap.repositories.inventory

import com.github.sophiecollard.bookswap.domain.inventory.{Copy, CopyStatus}
import com.github.sophiecollard.bookswap.domain.shared.Id

trait CopyRepository[F[_]] {

  def create(copy: Copy): F[Unit]

  def updateStatus(id: Id[Copy], newStatus: CopyStatus): F[Unit]

  def get(id: Id[Copy]): F[Option[Copy]]

}

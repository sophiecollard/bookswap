package com.github.sophiecollard.bookswap.repositories.transaction

import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.CopyRequest

trait CopyRequestRepository[F[_]] {

  def create(copyRequest: CopyRequest): F[Unit]

  def get(id: Id[CopyRequest]): F[Option[CopyRequest]]

}

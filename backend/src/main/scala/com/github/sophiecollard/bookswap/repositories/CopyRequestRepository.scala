package com.github.sophiecollard.bookswap.repositories

import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.CopyRequest

trait CopyRequestRepository[F[_]] {

  def get(id: Id[CopyRequest]): F[Option[CopyRequest]]

}

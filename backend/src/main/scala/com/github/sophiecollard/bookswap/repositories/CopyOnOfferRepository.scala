package com.github.sophiecollard.bookswap.repositories

import com.github.sophiecollard.bookswap.domain.inventory.CopyOnOffer
import com.github.sophiecollard.bookswap.domain.shared.Id

trait CopyOnOfferRepository[F[_]] {

  def get(id: Id[CopyOnOffer]): F[Option[CopyOnOffer]]

}

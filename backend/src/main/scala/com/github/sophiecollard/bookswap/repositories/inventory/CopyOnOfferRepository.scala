package com.github.sophiecollard.bookswap.repositories.inventory

import com.github.sophiecollard.bookswap.domain.inventory.{CopyOnOffer, CopyOnOfferStatus}
import com.github.sophiecollard.bookswap.domain.shared.Id

trait CopyOnOfferRepository[F[_]] {

  def create(copyOnOffer: CopyOnOffer): F[Unit]

  def updateStatus(id: Id[CopyOnOffer], newStatus: CopyOnOfferStatus): F[Unit]

  def get(id: Id[CopyOnOffer]): F[Option[CopyOnOffer]]

}

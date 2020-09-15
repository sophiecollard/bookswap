package com.github.sophiecollard.bookswap.domain.inventory

import java.time.LocalDateTime

import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User

final case class CopyOnOffer(
  id: Id[CopyOnOffer],
  edition: ISBN,
  offeredBy: Id[User],
  offeredOn: LocalDateTime,
  condition: Condition,
  status: CopyOnOfferStatus
)

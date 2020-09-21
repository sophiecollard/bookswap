package com.github.sophiecollard.bookswap.domain.transaction

import java.time.LocalDateTime

import com.github.sophiecollard.bookswap.domain.inventory.Copy
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User

final case class CopyRequest(
  id: Id[CopyRequest],
  copyId: Id[Copy],
  requestedBy: Id[User],
  requestedOn: LocalDateTime,
  status: RequestStatus
)

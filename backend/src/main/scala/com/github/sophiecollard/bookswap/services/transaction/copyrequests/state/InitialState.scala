package com.github.sophiecollard.bookswap.services.transaction.copyrequests.state

import com.github.sophiecollard.bookswap.domain.inventory.CopyStatus
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.{CopyRequest, RequestStatus}

final case class InitialState(
  requestStatus: RequestStatus,
  maybeNextRequest: Option[(Id[CopyRequest], RequestStatus)],
  copyStatus: CopyStatus
)

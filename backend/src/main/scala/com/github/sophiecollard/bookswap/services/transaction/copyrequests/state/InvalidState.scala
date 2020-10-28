package com.github.sophiecollard.bookswap.services.transaction.copyrequests.state

import com.github.sophiecollard.bookswap.domain.inventory.CopyStatus
import com.github.sophiecollard.bookswap.domain.transaction.RequestStatus

final case class InvalidState(requestStatus: RequestStatus, copyStatus: CopyStatus)

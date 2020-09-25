package com.github.sophiecollard.bookswap.services.inventory.copy.state

import com.github.sophiecollard.bookswap.domain.inventory.CopyStatus
import com.github.sophiecollard.bookswap.domain.transaction.RequestStatus

sealed trait StateUpdate

object StateUpdate {

  final case class UpdateCopyAndOpenRequestsStatuses(
    copyStatus: CopyStatus,
    openRequestsStatus: RequestStatus
  ) extends StateUpdate

  case object NoUpdate extends StateUpdate

  def updateCopyAndOpenRequestsStatuses(
    copyStatus: CopyStatus,
    openRequestsStatuses: RequestStatus
  ): StateUpdate =
    UpdateCopyAndOpenRequestsStatuses(copyStatus, openRequestsStatuses)

  def noUpdate: StateUpdate =
    NoUpdate

}

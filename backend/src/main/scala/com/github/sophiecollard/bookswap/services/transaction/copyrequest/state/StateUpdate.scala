package com.github.sophiecollard.bookswap.services.transaction.copyrequest.state

import com.github.sophiecollard.bookswap.domain.inventory.CopyStatus
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.{CopyRequest, RequestStatus}

sealed trait StateUpdate

object StateUpdate {

  final case class UpdateRequestStatus(requestStatus: RequestStatus) extends StateUpdate

  final case class UpdateRequestAndNextRequestStatuses(
    requestStatus: RequestStatus,
    nextRequestId: Id[CopyRequest],
    nextRequestStatus: RequestStatus
  ) extends StateUpdate

  final case class UpdateRequestAndCopyStatuses(
    requestStatus: RequestStatus,
    copyStatus: CopyStatus
  ) extends StateUpdate

  case object NoUpdate extends StateUpdate

  def updateRequestStatus(requestStatus: RequestStatus): StateUpdate =
    UpdateRequestStatus(requestStatus)

  def updateRequestAndNextRequestStatuses(
    requestStatus: RequestStatus,
    nextRequestId: Id[CopyRequest],
    nextRequestStatus: RequestStatus
  ): StateUpdate =
    UpdateRequestAndNextRequestStatuses(requestStatus, nextRequestId, nextRequestStatus)

  def updateRequestAndCopyStatuses(
    requestStatus: RequestStatus,
    copyStatus: CopyStatus
  ): StateUpdate =
    UpdateRequestAndCopyStatuses(requestStatus, copyStatus)

  def noUpdate: StateUpdate =
    NoUpdate

}

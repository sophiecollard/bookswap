package com.github.sophiecollard.bookswap.services.transaction.copyrequest.state

import java.time.ZoneId

import cats.implicits._
import com.github.sophiecollard.bookswap.domain.inventory.CopyStatus.{Available, Reserved, Swapped}
import com.github.sophiecollard.bookswap.domain.transaction.RequestStatus._
import com.github.sophiecollard.bookswap.syntax.JavaTimeSyntax.now

object StateMachine {

  def handleCancelCommand(initialState: InitialState)(implicit zoneId: ZoneId): Either[InvalidState, StateUpdate] =
    initialState match {
      // A CopyRequest that has already been Cancelled or Fulfilled cannot be Cancelled
      case InitialState(Cancelled(_), _, _) | InitialState(Fulfilled(_), _, Swapped) =>
        StateUpdate.noUpdate.asRight
      // A CopyRequest that has previously been Accepted is Cancelled
      // If there is another CopyRequest OnWaitingList, we Accept it
      case InitialState(Accepted(_), Some((nextRequestId, OnWaitingList(_))), Reserved) =>
        StateUpdate.updateRequestAndNextRequestStatuses(Cancelled(now), nextRequestId, Accepted(now)).asRight
      // Else, we update the Copy status back to Available
      case InitialState(Accepted(_), None, Reserved) =>
        StateUpdate.updateRequestAndCopyStatuses(Cancelled(now), Available).asRight
      // A CopyRequest that is Pending or OnWaitingList is Cancelled
      case InitialState(Pending, _, Available) |
           InitialState(Pending, _, Reserved) |
           InitialState(OnWaitingList(_), _, Reserved) =>
        StateUpdate.updateRequestStatus(Cancelled(now)).asRight
      case _ =>
        InvalidState(initialState.requestStatus, initialState.copyStatus).asLeft
    }

  def handleAcceptCommand(initialState: InitialState)(implicit zoneId: ZoneId): Either[InvalidState, StateUpdate] =
    initialState match {
      // A CopyRequest that has been Cancelled or Fulfilled cannot be Accepted
      case InitialState(Cancelled(_), _, _) | InitialState(Fulfilled(_), _, Swapped) =>
        StateUpdate.noUpdate.asRight
      // If the Copy is still Available, the CopyRequests is Accepted
      case InitialState(Pending, _, Available) | InitialState(Rejected(_), _, Available) =>
        StateUpdate.updateRequestAndCopyStatuses(Accepted(now), Reserved).asRight
      // If the Copy is already Reserved, the CopyRequest is put OnWaitingList
      case InitialState(Pending, _, Reserved) | InitialState(Rejected(_), _, Reserved) =>
        StateUpdate.updateRequestStatus(OnWaitingList(now)).asRight
      // A CopyRequest that has already been Accepted or OnWaitingList is left as-is
      case InitialState(Accepted(_), _, Reserved) | InitialState(OnWaitingList(_), _, Reserved) =>
        StateUpdate.noUpdate.asRight
      // All other state combinations are invalid in this scenario
      case _ =>
        InvalidState(initialState.requestStatus, initialState.copyStatus).asLeft
    }

  // NOTE This is exacly the same logic as for the Cancel command, only the current request's status update changes
  def handleRejectCommand(initialState: InitialState)(implicit zoneId: ZoneId): Either[InvalidState, StateUpdate] =
    initialState match {
      // A CopyRequest that has been Cancelled or Fulfilled cannot be Rejected
      case InitialState(Cancelled(_), _, _) | InitialState(Fulfilled(_), _, Swapped) =>
        StateUpdate.noUpdate.asRight
      // A CopyRequest that has previously been Accepted is Rejected
      // If there is another CopyRequest OnWaitingList, we Accept it
      case InitialState(Accepted(_), Some((nextRequestId, OnWaitingList(_))), Reserved) =>
        StateUpdate.updateRequestAndNextRequestStatuses(Rejected(now), nextRequestId, Accepted(now)).asRight
      // Else, we update the Copy status back to Available
      case InitialState(Accepted(_), None, Reserved) =>
        StateUpdate.updateRequestAndCopyStatuses(Rejected(now), Available).asRight
      // A CopyRequest that is Pending or OnWaitingList is Rejected
      case InitialState(Pending, _, Available) |
           InitialState(Pending, _, Reserved) |
           InitialState(OnWaitingList(_), _, Reserved) =>
        StateUpdate.updateRequestStatus(Rejected(now)).asRight
      case _ =>
        InvalidState(initialState.requestStatus, initialState.copyStatus).asLeft
    }

  def handleMarkAsFulfilledCommand(initialState: InitialState)(implicit zoneId: ZoneId): Either[InvalidState, StateUpdate] =
    initialState match {
      case InitialState(Accepted(_), _, Reserved) =>
        StateUpdate.updateRequestAndCopyStatuses(Fulfilled(now), Swapped).asRight
      case _ =>
        InvalidState(initialState.requestStatus, initialState.copyStatus).asLeft
    }

}

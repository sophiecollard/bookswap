package com.github.sophiecollard.bookswap.services.transaction.copyrequests.state

import java.time.ZoneId

import cats.implicits._
import com.github.sophiecollard.bookswap.domain.inventory.CopyStatus.{Available, Reserved, Swapped}
import com.github.sophiecollard.bookswap.domain.transaction.RequestStatus._
import com.github.sophiecollard.bookswap.syntax.JavaTimeSyntax.now

object StateMachine {

  def handleCancelCommand(initialState: InitialState)(implicit zoneId: ZoneId): Either[InvalidState, StateUpdate] =
    initialState match {
      case InitialState(Accepted(_), Some((nextRequestId, OnWaitingList(_))), Reserved) =>
        StateUpdate.updateRequestAndNextRequestStatuses(Cancelled(now), nextRequestId, Accepted(now)).asRight
      case InitialState(Accepted(_), None, Reserved) =>
        StateUpdate.updateRequestAndCopyStatuses(Cancelled(now), Available).asRight
      case InitialState(Pending, _, Available) |
           InitialState(Pending, _, Reserved) |
           InitialState(OnWaitingList(_), _, Reserved) |
           InitialState(Rejected(_), _, _) =>
        StateUpdate.updateRequestStatus(Cancelled(now)).asRight
      case InitialState(Cancelled(_), _, _) |
           InitialState(Fulfilled(_), _, Swapped) =>
        StateUpdate.noUpdate.asRight
      case _ =>
        InvalidState(initialState.requestStatus, initialState.copyStatus).asLeft
    }

  def handleAcceptCommand(initialState: InitialState)(implicit zoneId: ZoneId): Either[InvalidState, StateUpdate] =
    initialState match {
      case InitialState(Pending, _, Available) | InitialState(Rejected(_), _, Available) =>
        StateUpdate.updateRequestAndCopyStatuses(Accepted(now), Reserved).asRight
      case InitialState(Pending, _, Reserved) | InitialState(Rejected(_), _, Reserved) =>
        StateUpdate.updateRequestStatus(OnWaitingList(now)).asRight
      case InitialState(Accepted(_), _, Reserved) |
           InitialState(OnWaitingList(_), _, Reserved) |
           InitialState(Rejected(_), _, _) |
           InitialState(Cancelled(_), _, _) |
           InitialState(Fulfilled(_), _, Swapped) =>
        StateUpdate.noUpdate.asRight
      case _ =>
        InvalidState(initialState.requestStatus, initialState.copyStatus).asLeft
    }

  def handleRejectCommand(initialState: InitialState)(implicit zoneId: ZoneId): Either[InvalidState, StateUpdate] =
    initialState match {
      case InitialState(Accepted(_), Some((nextRequestId, OnWaitingList(_))), Reserved) =>
        StateUpdate.updateRequestAndNextRequestStatuses(Rejected(now), nextRequestId, Accepted(now)).asRight
      case InitialState(Accepted(_), None, Reserved) =>
        StateUpdate.updateRequestAndCopyStatuses(Rejected(now), Available).asRight
      case InitialState(Pending, _, Available) |
           InitialState(Pending, _, Reserved) |
           InitialState(OnWaitingList(_), _, Reserved) =>
        StateUpdate.updateRequestStatus(Rejected(now)).asRight
      case InitialState(Rejected(_), _, _) |
           InitialState(Cancelled(_), _, _) |
           InitialState(Fulfilled(_), _, Swapped) =>
        StateUpdate.noUpdate.asRight
      case _ =>
        InvalidState(initialState.requestStatus, initialState.copyStatus).asLeft
    }

  def handleMarkAsFulfilledCommand(initialState: InitialState)(implicit zoneId: ZoneId): Either[InvalidState, StateUpdate] =
    initialState match {
      case InitialState(Accepted(_), _, Reserved) =>
        StateUpdate.updateRequestAndOpenRequestsAndCopyStatuses(Fulfilled(now), Rejected(now), Swapped).asRight
      case InitialState(Pending, _, Available) |
           InitialState(Pending, _, Reserved) |
           InitialState(OnWaitingList(_), _, Reserved) |
           InitialState(Rejected(_), _, _) |
           InitialState(Cancelled(_), _, _) |
           InitialState(Fulfilled(_), _, Swapped) =>
        StateUpdate.noUpdate.asRight
      case _ =>
        InvalidState(initialState.requestStatus, initialState.copyStatus).asLeft
    }

}

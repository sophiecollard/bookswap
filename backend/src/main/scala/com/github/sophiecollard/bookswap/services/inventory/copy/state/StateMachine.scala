package com.github.sophiecollard.bookswap.services.inventory.copy.state

import java.time.ZoneId

import com.github.sophiecollard.bookswap.domain.inventory.CopyStatus.{Available, Reserved, Swapped, Withdrawn}
import com.github.sophiecollard.bookswap.domain.transaction.RequestStatus
import com.github.sophiecollard.bookswap.syntax.JavaTimeSyntax.now

object StateMachine {

  def handleWithdrawCommand(initialState: InitialState)(implicit zoneId: ZoneId): StateUpdate =
    initialState match {
      case InitialState(Available) | InitialState(Reserved) =>
        StateUpdate.updateCopyAndOpenRequestsStatuses(Withdrawn, RequestStatus.rejected(now))
      case InitialState(Swapped) | InitialState(Withdrawn) =>
        StateUpdate.noUpdate
    }

}

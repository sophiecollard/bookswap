package com.github.sophiecollard.bookswap.api.transaction.copyrequests

import enumeratum.values.{StringCirceEnum, StringEnum, StringEnumEntry}

sealed abstract class Command(val value: String) extends StringEnumEntry

object Command extends StringEnum[Command] with StringCirceEnum[Command] {

  case object Accept extends Command("accept")
  case object Reject extends Command("reject")
  case object MarkAsFulfilled extends Command("mark_as_fulfilled")

  override val values: IndexedSeq[Command] = findValues

}

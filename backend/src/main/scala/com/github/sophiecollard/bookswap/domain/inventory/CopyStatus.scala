package com.github.sophiecollard.bookswap.domain.inventory

import enumeratum.values.{StringDoobieEnum, StringEnum, StringEnumEntry}

import scala.collection.immutable

sealed abstract class CopyStatus(val value: String) extends StringEnumEntry

object CopyStatus extends StringEnum[CopyStatus] with StringDoobieEnum[CopyStatus] {

  case object Available extends CopyStatus("available")
  case object Reserved  extends CopyStatus("reserved")
  case object Swapped   extends CopyStatus("swapped")
  case object Withdrawn extends CopyStatus("withdrawn")

  override val values: immutable.IndexedSeq[CopyStatus] = findValues

}

package com.github.sophiecollard.bookswap.domain.inventory

import enumeratum.values.{StringEnum, StringEnumEntry}

import scala.collection.immutable

sealed abstract class CopyOnOfferStatus(val value: String) extends StringEnumEntry

object CopyOnOfferStatus extends StringEnum[CopyOnOfferStatus] {

  case object Available extends CopyOnOfferStatus("available")
  case object Reserved  extends CopyOnOfferStatus("reserved")
  case object Swapped   extends CopyOnOfferStatus("swapped")
  case object Withdrawn extends CopyOnOfferStatus("withdrawn")

  override val values: immutable.IndexedSeq[CopyOnOfferStatus] = findValues

}

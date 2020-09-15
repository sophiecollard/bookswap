package com.github.sophiecollard.bookswap.domain.inventory

import enumeratum.values.{StringEnum, StringEnumEntry}

import scala.collection.immutable

sealed abstract class CopyOnRequestStatus(val value: String) extends StringEnumEntry

object CopyOnRequestStatus extends StringEnum[CopyOnRequestStatus] {

  case object Requested extends CopyOnRequestStatus("requested")
  case object Fulfilled extends CopyOnRequestStatus("fulfilled")
  case object Expired   extends CopyOnRequestStatus("expired")

  override val values: immutable.IndexedSeq[CopyOnRequestStatus] = findValues

}

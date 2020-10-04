package com.github.sophiecollard.bookswap.api.model.inventory

import com.github.sophiecollard.bookswap.api.Converter
import com.github.sophiecollard.bookswap.domain
import enumeratum.values.{StringCirceEnum, StringEnum, StringEnumEntry}

import scala.collection.immutable

sealed abstract class CopyStatus(val value: String) extends StringEnumEntry

object CopyStatus extends StringEnum[CopyStatus] with StringCirceEnum[CopyStatus] {

  case object Available extends CopyStatus("available")
  case object Reserved  extends CopyStatus("reserved")
  case object Swapped   extends CopyStatus("swapped")
  case object Withdrawn extends CopyStatus("withdrawn")

  override val values: immutable.IndexedSeq[CopyStatus] = findValues

  def available: CopyStatus = Available
  def reserved: CopyStatus = Reserved
  def swapped: CopyStatus = Swapped
  def withdrawn: CopyStatus = Withdrawn

  implicit val converterTo: Converter[CopyStatus, domain.inventory.CopyStatus] =
    Converter.instance {
      case Available => domain.inventory.CopyStatus.Available
      case Reserved  => domain.inventory.CopyStatus.Reserved
      case Swapped   => domain.inventory.CopyStatus.Swapped
      case Withdrawn => domain.inventory.CopyStatus.Withdrawn
    }

  implicit val converterFrom: Converter[domain.inventory.CopyStatus, CopyStatus] =
    Converter.instance {
      case domain.inventory.CopyStatus.Available => Available
      case domain.inventory.CopyStatus.Reserved  => Reserved
      case domain.inventory.CopyStatus.Swapped   => Swapped
      case domain.inventory.CopyStatus.Withdrawn => Withdrawn
    }

}

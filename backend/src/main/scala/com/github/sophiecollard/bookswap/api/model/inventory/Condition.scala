package com.github.sophiecollard.bookswap.api.model.inventory

import com.github.sophiecollard.bookswap.api.Converter
import com.github.sophiecollard.bookswap.domain
import enumeratum.values.{StringCirceEnum, StringEnum, StringEnumEntry}

import scala.collection.immutable

sealed abstract class Condition(val value: String) extends StringEnumEntry

object Condition extends StringEnum[Condition] with StringCirceEnum[Condition] {

  case object BrandNew       extends Condition("brand_new")
  case object Good           extends Condition("good")
  case object SomeSignsOfUse extends Condition("some_signs_of_use")
  case object Poor           extends Condition("poor")

  override val values: immutable.IndexedSeq[Condition] = findValues

  implicit val converterTo: Converter[Condition, domain.inventory.Condition] =
    Converter.instance {
      case BrandNew       => domain.inventory.Condition.BrandNew
      case Good           => domain.inventory.Condition.Good
      case SomeSignsOfUse => domain.inventory.Condition.SomeSignsOfUse
      case Poor           => domain.inventory.Condition.Poor
    }

  implicit val converterFrom: Converter[domain.inventory.Condition, Condition] =
    Converter.instance {
      case domain.inventory.Condition.BrandNew       => BrandNew
      case domain.inventory.Condition.Good           => Good
      case domain.inventory.Condition.SomeSignsOfUse => SomeSignsOfUse
      case domain.inventory.Condition.Poor           => Poor
    }

}

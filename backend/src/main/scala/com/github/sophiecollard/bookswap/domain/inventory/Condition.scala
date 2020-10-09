package com.github.sophiecollard.bookswap.domain.inventory

import enumeratum.values.{StringCirceEnum, StringDoobieEnum, StringEnum, StringEnumEntry}

import scala.collection.immutable

sealed abstract class Condition(val value: String) extends StringEnumEntry

object Condition
  extends StringEnum[Condition]
    with StringCirceEnum[Condition]
    with StringDoobieEnum[Condition] {

  case object BrandNew       extends Condition("brand_new")
  case object Good           extends Condition("good")
  case object SomeSignsOfUse extends Condition("some_signs_of_use")
  case object Poor           extends Condition("poor")

  override val values: immutable.IndexedSeq[Condition] = findValues

}

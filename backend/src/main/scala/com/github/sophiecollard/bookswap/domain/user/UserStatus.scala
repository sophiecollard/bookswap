package com.github.sophiecollard.bookswap.domain.user

import enumeratum.values.{StringEnum, StringEnumEntry}

import scala.collection.immutable

sealed abstract class UserStatus(val value: String) extends StringEnumEntry

object UserStatus extends StringEnum[UserStatus] {

  case object PendingVerification extends UserStatus("pending_verification")
  case object Active              extends UserStatus("active")
  case object Banned              extends UserStatus("banned")
  case object Deleted             extends UserStatus("deleted")
  case object Admin               extends UserStatus("admin")

  val values: immutable.IndexedSeq[UserStatus] = findValues

}

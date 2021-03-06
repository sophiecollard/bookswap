package com.github.sophiecollard.bookswap.domain.user

import enumeratum.values.{StringCirceEnum, StringDoobieEnum, StringEnum, StringEnumEntry}

import scala.collection.immutable

sealed abstract class UserStatus(val value: String) extends StringEnumEntry

object UserStatus
  extends StringEnum[UserStatus]
    with StringCirceEnum[UserStatus]
    with StringDoobieEnum[UserStatus] {

  case object PendingVerification extends UserStatus("pending_verification")
  case object Active              extends UserStatus("active")
  case object Admin               extends UserStatus("admin")
  case object Banned              extends UserStatus("banned")
  case object PendingDeletion     extends UserStatus("pending_deletion")

  val values: immutable.IndexedSeq[UserStatus] = findValues

}

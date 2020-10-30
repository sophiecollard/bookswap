package com.github.sophiecollard.bookswap.domain.user

import scala.util.Try

final class PasswordHash(private val value: String) {
  override def toString: String =
    "[REDACTED]"

  override def hashCode(): Int =
    value.hashCode

  override def equals(obj: Any): Boolean =
    Try(obj.asInstanceOf[PasswordHash])
      .toOption
      .exists(_.value == value)
}

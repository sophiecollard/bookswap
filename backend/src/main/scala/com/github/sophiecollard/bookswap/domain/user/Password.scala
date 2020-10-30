package com.github.sophiecollard.bookswap.domain.user

final class Password(private val value: String) {
  override def toString: String =
    "[REDACTED]"

  override def hashCode(): Int =
    toString.hashCode

  override def equals(obj: Any): Boolean =
    false
}

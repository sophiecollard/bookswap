package com.github.sophiecollard.bookswap.domain.shared

import doobie.Meta

final case class Name[A](value: String)

object Name {

  implicit def meta[A]: Meta[Name[A]] =
    Meta[String].imap(apply[A])(_.value)

}

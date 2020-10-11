package com.github.sophiecollard.bookswap.domain.shared

import doobie.Meta
import io.circe.{Decoder, Encoder}

final case class Name[A](value: String)

object Name {

  implicit def meta[A]: Meta[Name[A]] =
    Meta[String].imap(apply[A])(_.value)

  implicit def encoder[A]: Encoder[Name[A]] =
    Encoder[String].contramap(_.value)

  implicit def decoder[A]: Decoder[Name[A]] =
    Decoder[String].map(apply[A])

}

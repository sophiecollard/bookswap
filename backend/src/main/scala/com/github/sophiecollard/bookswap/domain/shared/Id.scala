package com.github.sophiecollard.bookswap.domain.shared

import java.util.UUID

import doobie.util.meta.Meta
import io.circe.{Decoder, Encoder}

final case class Id[A](value: UUID)

object Id {

  def generate[A]: Id[A] =
    Id(UUID.randomUUID())

  implicit val uuidMeta: Meta[UUID] =
    Meta[String].imap(UUID.fromString)(_.toString)

  implicit def meta[A]: Meta[Id[A]] =
    Meta[UUID].imap(apply[A])(_.value)

  implicit val uuidEncoder: Encoder[UUID] =
    Encoder[String].contramap(_.toString)

  implicit def encoder[A]: Encoder[Id[A]] =
    Encoder[UUID].contramap(_.value)

  implicit val uuidDecoder: Decoder[UUID] =
    Decoder[String].map(UUID.fromString)

  implicit def decoder[A]: Decoder[Id[A]] =
    Decoder[UUID].map(apply[A])

}

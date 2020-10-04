package com.github.sophiecollard.bookswap.api.model.shared

import java.util.UUID

import com.github.sophiecollard.bookswap.api.Converter
import com.github.sophiecollard.bookswap.domain
import io.circe.{Decoder, Encoder}

final case class Id[A](value: UUID)

object Id {

  implicit val uuidEncoder: Encoder[UUID] =
    Encoder[String].contramap(_.toString)

  implicit def encoder[A]: Encoder[Id[A]] =
    Encoder[UUID].contramap(_.value)

  implicit val uuidDecoder: Decoder[UUID] =
    Decoder[String].map(UUID.fromString)

  implicit def decoder[A]: Decoder[Id[A]] =
    Decoder[UUID].map(apply[A])

  implicit def converterFrom[A]: Converter[domain.shared.Id[A], Id[A]] =
    Converter.instance(id => apply[A](id.value))

}

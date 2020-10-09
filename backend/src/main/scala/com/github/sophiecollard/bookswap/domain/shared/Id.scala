package com.github.sophiecollard.bookswap.domain.shared

import java.util.UUID

import com.github.sophiecollard.bookswap.api.instances.uuid._
import com.github.sophiecollard.bookswap.repositories.instances.uuid._
import doobie.util.meta.Meta
import io.circe.{Decoder, Encoder}
import org.http4s.QueryParamDecoder

final case class Id[A](value: UUID)

object Id {

  def generate[A]: Id[A] =
    Id(UUID.randomUUID())

  implicit def meta[A]: Meta[Id[A]] =
    Meta[UUID].imap(apply[A])(_.value)

  implicit def encoder[A]: Encoder[Id[A]] =
    Encoder[UUID].contramap(_.value)

  implicit def decoder[A]: Decoder[Id[A]] =
    Decoder[UUID].map(apply[A])

  implicit def queryParamDecoder[A]: QueryParamDecoder[Id[A]] =
    QueryParamDecoder[UUID].map(apply[A])

}

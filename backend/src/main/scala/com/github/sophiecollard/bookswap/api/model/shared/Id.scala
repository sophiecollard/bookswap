package com.github.sophiecollard.bookswap.api.model.shared

import java.util.UUID

import com.github.sophiecollard.bookswap.api.Converter
import com.github.sophiecollard.bookswap.api.instances._
import com.github.sophiecollard.bookswap.domain
import io.circe.{Decoder, Encoder}
import org.http4s.QueryParamDecoder

final case class Id[A](value: UUID)

object Id {

  implicit def encoder[A]: Encoder[Id[A]] =
    Encoder[UUID].contramap(_.value)

  implicit def decoder[A]: Decoder[Id[A]] =
    Decoder[UUID].map(apply[A])

  implicit def queryParamDecoder[A]: QueryParamDecoder[Id[A]] =
    QueryParamDecoder[UUID].map(apply[A])

  implicit def converterTo[A]: Converter[Id[A], domain.shared.Id[A]] =
    Converter.instance(id => domain.shared.Id[A](id.value))

  implicit def converterFrom[A]: Converter[domain.shared.Id[A], Id[A]] =
    Converter.instance(id => apply[A](id.value))

}

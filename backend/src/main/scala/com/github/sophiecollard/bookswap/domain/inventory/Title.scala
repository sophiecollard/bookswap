package com.github.sophiecollard.bookswap.domain.inventory

import doobie.util.meta.Meta
import io.circe.{Decoder, Encoder}

final case class Title(value: String)

object Title {

  implicit val meta: Meta[Title] =
    Meta[String].imap(apply)(_.value)

  implicit val encoder: Encoder[Title] =
    Encoder[String].contramap(_.value)

  implicit val decoder: Decoder[Title] =
    Decoder[String].map(apply)

}

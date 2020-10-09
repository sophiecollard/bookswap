package com.github.sophiecollard.bookswap.api.instances

import java.util.UUID

import cats.implicits._
import io.circe.{Decoder, Encoder}
import org.http4s.{ParseFailure, QueryParamDecoder}

import scala.util.Try

trait uuid {

  implicit val uuidEncoder: Encoder[UUID] =
    Encoder[String].contramap(_.toString)

  implicit val uuidDecoder: Decoder[UUID] =
    Decoder[String].emap { string =>
      Try(UUID.fromString(string))
        .toEither
        .leftMap(_ => s"Failed to decode java.util.UUID from $string")
    }

  implicit val uuidQueryParamDecoder: QueryParamDecoder[UUID] =
    QueryParamDecoder[String].emap { string =>
      Try(UUID.fromString(string))
        .toEither
        .leftMap(_ => ParseFailure(string, s"Failed to parse java.util.UUID from $string"))
    }

}

object uuid extends uuid

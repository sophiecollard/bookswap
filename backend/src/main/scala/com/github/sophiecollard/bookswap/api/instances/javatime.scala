package com.github.sophiecollard.bookswap.api.instances

import java.time.LocalDateTime

import cats.implicits._
import org.http4s.{ParseFailure, QueryParamDecoder}

import scala.util.Try

trait javatime {

  implicit val localDateTimeQueryParamDecoder: QueryParamDecoder[LocalDateTime] =
    QueryParamDecoder[String].emap { string =>
      Try(LocalDateTime.parse(string))
        .toEither
        .leftMap(_ => ParseFailure(string, s"Failed to parse java.time.LocalDateTime from $string"))
    }

}

object javatime extends javatime

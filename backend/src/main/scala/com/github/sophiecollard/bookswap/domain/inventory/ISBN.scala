package com.github.sophiecollard.bookswap.domain.inventory

import doobie.util.meta.Meta
import io.circe.{Decoder, Encoder}
import org.http4s.{ParseFailure, QueryParamDecoder}

sealed abstract case class ISBN(value: String)

object ISBN {

  def apply(value: String): Option[ISBN] = {
    val isbnPattern = "^[0-9]{13}$".r
    isbnPattern findFirstIn value map {
      new ISBN(_) {}
    }
  }

  def unsafeApply(value: String): ISBN =
    apply(value).get

  implicit val meta: Meta[ISBN] =
    Meta[String].imap(unsafeApply)(_.value)

  implicit val encoder: Encoder[ISBN] =
    Encoder[String].contramap(_.value)

  implicit val decoder: Decoder[ISBN] =
    Decoder[String].emap { string =>
      apply(string) match {
        case Some(isbn) => Right(isbn)
        case None       => Left(s"Failed to parse ISBN from $string")
      }
    }

  implicit val queryParamDecoder: QueryParamDecoder[ISBN] =
    QueryParamDecoder[String].emap { string =>
      apply(string) match {
        case Some(isbn) => Right(isbn)
        case None       => Left(ParseFailure(string, s"Failed to parse ISBN from $string"))
      }
    }

}

package com.github.sophiecollard.bookswap.domain.inventory

import doobie.util.meta.Meta
import io.circe.{Decoder, Encoder}
import org.http4s.{ParseFailure, QueryParamDecoder}

sealed trait ISBN { def value: String }

object ISBN {

  sealed abstract case class ThirteenDigits(value: String) extends ISBN

  object ThirteenDigits {
    def apply(value: String): Option[ThirteenDigits] = {
      val thirteenDigitsPattern = "^(978|979)[0-9]{10}$".r
      thirteenDigitsPattern findFirstIn value map {
        new ThirteenDigits(_) {}
      }
    }
  }

  sealed abstract case class TenDigits(value: String) extends ISBN

  object TenDigits {
    def apply(value: String): Option[TenDigits] = {
      val tenDigitsPattern = "^[0-9]{10}$".r
      tenDigitsPattern findFirstIn value map {
        new TenDigits(_) {}
      }
    }
  }

  def apply(value: String): Option[ISBN] =
    ThirteenDigits(value) orElse TenDigits(value)

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

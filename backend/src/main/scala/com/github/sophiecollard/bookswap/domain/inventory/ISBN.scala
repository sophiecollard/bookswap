package com.github.sophiecollard.bookswap.domain.inventory

import cats.syntax.option._
import doobie.util.meta.Meta
import io.circe.{Decoder, Encoder}
import org.http4s.{ParseFailure, QueryParamDecoder}

sealed trait ISBN {

  /** Returns the ISBN as a String stripped of dashes and spaces */
  def value: String

  /** Returns the 3-digit prefix of a 13-digit ISBN. Defaults to '978' for 10-digit ISBNs. */
  def prefix: ISBN.EAN

  /**
    * Returns the language a book was most probably published in based on the ISBN group.
    *
    * Note that ISBN groups are typically assigned to countries or geographical areas. As such, they only provide an
    * educated guess as to which language a book was published in.
    */
  final def language: Option[Language] =
    prefix match {
      case ISBN.EAN.`978` => languageFor978Prefix
      case ISBN.EAN.`979` => languageFor979Prefix
    }

  private def withoutPrefix: ISBN.TenDigit =
    this match {
      case ISBN.ThirteenDigit(_, tenDigit) => tenDigit
      case tenDigit: ISBN.TenDigit         => tenDigit
    }

  private def languageFor978Prefix: Option[Language] =
    withoutPrefix.value.toList match {
      case '0' :: _ => Language.English.some
      case '1' :: _ => Language.English.some
      case '2' :: _ => Language.French.some
      case '3' :: _ => Language.German.some
      case '4' :: _ => Language.Japanese.some
      case '5' :: _ => Language.Russian.some
      case '7' :: _ => Language.Chinese.some
      case _        => None
    }

  private def languageFor979Prefix: Option[Language] =
    withoutPrefix.value.toList match {
      case '8' :: _        => Language.English.some
      case '1' :: '0' :: _ => Language.French.some
      case '1' :: '1' :: _ => Language.Korean.some
      case '1' :: '2' :: _ => Language.Italian.some
      case _               => None
    }

}

object ISBN {

  sealed abstract class EAN(val value: String)

  object EAN {

    case object `978` extends EAN("978")
    case object `979` extends EAN("979")

    def apply(value: String): Option[EAN] =
      value match {
        case "978" => `978`.some
        case "979" => `979`.some
        case _     => None
      }

  }

  sealed abstract case class ThirteenDigit(prefix: EAN, tenDigit: TenDigit) extends ISBN {
    override def value: String =
      s"${prefix.value}${tenDigit.value}"
  }

  object ThirteenDigit {
    def apply(value: String): Option[ThirteenDigit] = {
      val strippedValue = value.replaceAll("(-| )", "")
      for {
        prefix <- EAN(strippedValue.take(3))
        remainder <- TenDigit(strippedValue.drop(3))
      } yield new ThirteenDigit(prefix, remainder) {}
    }
  }

  sealed abstract case class TenDigit(value: String) extends ISBN {
    override def prefix: EAN =
      EAN.`978`
  }

  object TenDigit {
    def apply(value: String): Option[TenDigit] = {
      val tenDigitPattern = "^[0-9]{10}$".r
      val strippedValue = value.replaceAll("(-| )", "")
      tenDigitPattern findFirstIn strippedValue map {
        new TenDigit(_) {}
      }
    }
  }

  def apply(value: String): Option[ISBN] =
    ThirteenDigit(value) orElse TenDigit(value)

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

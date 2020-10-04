package com.github.sophiecollard.bookswap.api.model.inventory

import io.circe.{Decoder, Encoder}

sealed abstract case class ISBN(value: String)

object ISBN {

  def apply(value: String): Option[ISBN] = {
    val isbnPattern = "^[0-9]{13}$".r
    isbnPattern findFirstIn value map {
      new ISBN(_) {}
    }
  }

  implicit val encoder: Encoder[ISBN] =
    Encoder[String].contramap(_.value)

  implicit val decoder: Decoder[ISBN] =
    Decoder[String].emap { string =>
      apply(string) match {
        case Some(isbn) => Right(isbn)
        case None       => Left(s"Failed to parse ISBN from $string")
      }
    }

}

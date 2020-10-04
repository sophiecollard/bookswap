package com.github.sophiecollard.bookswap.api.model.inventory

import com.github.sophiecollard.bookswap.api.Converter
import com.github.sophiecollard.bookswap.domain
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

  implicit val converterTo: Converter[ISBN, domain.inventory.ISBN] =
    Converter.instance { isbn =>
      domain.inventory.ISBN.unvalidated(isbn.value)
    }

  implicit val converterFrom: Converter[domain.inventory.ISBN, ISBN] =
    Converter.instance { isbn =>
      new ISBN(isbn.value) {}
    }

}

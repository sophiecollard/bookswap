package com.github.sophiecollard.bookswap.api.inventory.editions

import java.time.LocalDate

import cats.data.NonEmptyList
import com.github.sophiecollard.bookswap.api.Converter
import com.github.sophiecollard.bookswap.domain.inventory.{Author, Edition, ISBN, Publisher, Title}
import com.github.sophiecollard.bookswap.domain.shared.Id
import io.circe.Encoder
import io.circe.generic.semiauto

final case class EditionResponseBody(
  isbn: ISBN,
  title: Title,
  authorIds: NonEmptyList[Id[Author]],
  publisherId: Option[Id[Publisher]],
  publicationDate: Option[LocalDate]
)

object EditionResponseBody {

  implicit val encoder: Encoder[EditionResponseBody] =
    semiauto.deriveEncoder

  implicit val converter: Converter[Edition, EditionResponseBody] =
    Converter.instance { edition =>
      EditionResponseBody(
        isbn = edition.isbn,
        title = edition.title,
        authorIds = edition.authorIds,
        publisherId = edition.publisherId,
        publicationDate = edition.publicationDate
      )
    }

}

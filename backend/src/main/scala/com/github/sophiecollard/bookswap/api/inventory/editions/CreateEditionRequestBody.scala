package com.github.sophiecollard.bookswap.api.inventory.editions

import java.time.LocalDate

import cats.data.NonEmptyList
import com.github.sophiecollard.bookswap.api.Converter
import com.github.sophiecollard.bookswap.domain.inventory.{Author, Edition, ISBN, Publisher, Title}
import com.github.sophiecollard.bookswap.domain.shared.Id
import io.circe.Decoder
import io.circe.generic.semiauto

final case class CreateEditionRequestBody(
  isbn: ISBN,
  title: Title,
  authorIds: NonEmptyList[Id[Author]],
  publisherId: Option[Id[Publisher]],
  publicationDate: Option[LocalDate]
)

object CreateEditionRequestBody {

  implicit val decoder: Decoder[CreateEditionRequestBody] =
    semiauto.deriveDecoder

  implicit val converter: Converter[CreateEditionRequestBody, Edition] =
    Converter.instance { requestBody =>
      Edition(
        isbn = requestBody.isbn,
        title = requestBody.title,
        authorIds = requestBody.authorIds,
        publisherId = requestBody.publisherId,
        publicationDate = requestBody.publicationDate
      )
    }

}

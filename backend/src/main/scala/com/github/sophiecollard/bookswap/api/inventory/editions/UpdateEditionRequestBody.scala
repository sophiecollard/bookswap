package com.github.sophiecollard.bookswap.api.inventory.editions

import java.time.LocalDate

import cats.data.NonEmptyList
import com.github.sophiecollard.bookswap.api.Converter
import com.github.sophiecollard.bookswap.domain.inventory.{Author, EditionDetailsUpdate, Publisher, Title}
import com.github.sophiecollard.bookswap.domain.shared.{Id, MaybeUpdate}
import io.circe.Decoder
import io.circe.generic.semiauto

final case class UpdateEditionRequestBody(
  title: MaybeUpdate[Title],
  authorIds: MaybeUpdate[NonEmptyList[Id[Author]]],
  publisherId: MaybeUpdate[Option[Id[Publisher]]],
  publicationDate: MaybeUpdate[Option[LocalDate]]
)

object UpdateEditionRequestBody {

  implicit val decoder: Decoder[UpdateEditionRequestBody] =
    semiauto.deriveDecoder

  implicit val converter: Converter[UpdateEditionRequestBody, EditionDetailsUpdate] =
    Converter.instance { request =>
      EditionDetailsUpdate(
        title = request.title,
        authorIds = request.authorIds,
        publisherId = request.publisherId,
        publicationDate = request.publicationDate
      )
    }

}

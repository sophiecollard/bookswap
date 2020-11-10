package com.github.sophiecollard.bookswap.api.inventory.authors

import com.github.sophiecollard.bookswap.api.Converter
import com.github.sophiecollard.bookswap.domain.inventory.Author
import com.github.sophiecollard.bookswap.domain.shared.Name
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto

final case class CreateAuthorRequestBody(name: Name[Author])

object CreateAuthorRequestBody {

  implicit val encoder: Encoder[CreateAuthorRequestBody] =
    semiauto.deriveEncoder

  implicit val decoder: Decoder[CreateAuthorRequestBody] =
    semiauto.deriveDecoder

  implicit val converter: Converter[CreateAuthorRequestBody, Name[Author]] =
    Converter.instance(_.name)

}

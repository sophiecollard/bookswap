package com.github.sophiecollard.bookswap.api.inventory.copies

import com.github.sophiecollard.bookswap.api.Converter
import com.github.sophiecollard.bookswap.domain.inventory.Condition
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto

final case class UpdateCopyRequestBody(condition: Condition)

object UpdateCopyRequestBody {

  implicit val encoder: Encoder[UpdateCopyRequestBody] =
    semiauto.deriveEncoder

  implicit val decoder: Decoder[UpdateCopyRequestBody] =
    semiauto.deriveDecoder

  implicit val converter: Converter[UpdateCopyRequestBody, Condition] =
    Converter.instance(_.condition)

}

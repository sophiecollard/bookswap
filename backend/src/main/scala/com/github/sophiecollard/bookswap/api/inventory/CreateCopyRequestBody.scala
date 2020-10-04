package com.github.sophiecollard.bookswap.api.inventory

import com.github.sophiecollard.bookswap.api.model.inventory.{Condition, ISBN}
import io.circe.Decoder
import io.circe.generic.semiauto

final case class CreateCopyRequestBody(
  isbn: ISBN,
  condition: Condition
)

object CreateCopyRequestBody {

  implicit val decoder: Decoder[CreateCopyRequestBody] =
    semiauto.deriveDecoder

}

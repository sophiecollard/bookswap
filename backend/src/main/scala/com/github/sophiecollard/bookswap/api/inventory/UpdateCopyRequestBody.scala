package com.github.sophiecollard.bookswap.api.inventory

import com.github.sophiecollard.bookswap.api.model.inventory.Condition
import io.circe.Decoder
import io.circe.generic.semiauto

final case class UpdateCopyRequestBody(condition: Condition)

object UpdateCopyRequestBody {

  implicit val decoder: Decoder[UpdateCopyRequestBody] =
    semiauto.deriveDecoder

}

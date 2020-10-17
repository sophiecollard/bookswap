package com.github.sophiecollard.bookswap.api.transaction.copyrequests

import io.circe.Decoder
import io.circe.generic.semiauto

final case class UpdateCopyRequestRequestBody(command: Command)

object UpdateCopyRequestRequestBody {

  implicit val decoder: Decoder[UpdateCopyRequestRequestBody] =
    semiauto.deriveDecoder

}

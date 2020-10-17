package com.github.sophiecollard.bookswap.api.transaction.copyrequests

import com.github.sophiecollard.bookswap.api.Converter
import com.github.sophiecollard.bookswap.domain.inventory.Copy
import com.github.sophiecollard.bookswap.domain.shared.Id
import io.circe.Decoder
import io.circe.generic.semiauto

final case class CreateCopyRequestRequestBody(
  copyId: Id[Copy]
)

object CreateCopyRequestRequestBody {

  implicit val decoder: Decoder[CreateCopyRequestRequestBody] =
    semiauto.deriveDecoder

  implicit val converter: Converter[CreateCopyRequestRequestBody, Id[Copy]] =
    Converter.instance(_.copyId)

}

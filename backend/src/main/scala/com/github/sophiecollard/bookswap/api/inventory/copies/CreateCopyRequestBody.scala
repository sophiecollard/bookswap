package com.github.sophiecollard.bookswap.api.inventory.copies

import com.github.sophiecollard.bookswap.api.Converter
import com.github.sophiecollard.bookswap.domain.inventory.{Condition, ISBN}
import io.circe.Decoder
import io.circe.generic.semiauto

final case class CreateCopyRequestBody(
  isbn: ISBN,
  condition: Condition
)

object CreateCopyRequestBody {

  implicit val decoder: Decoder[CreateCopyRequestBody] =
    semiauto.deriveDecoder

  implicit val converter: Converter[CreateCopyRequestBody, (ISBN, Condition)] =
    Converter.instance { requestBody =>
      (
        requestBody.isbn,
        requestBody.condition
      )
    }

}

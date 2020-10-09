package com.github.sophiecollard.bookswap.api.inventory.copy

import com.github.sophiecollard.bookswap.api.Converter
import com.github.sophiecollard.bookswap.api.model.inventory.Condition
import com.github.sophiecollard.bookswap.api.syntax._
import com.github.sophiecollard.bookswap.domain
import com.github.sophiecollard.bookswap.domain.inventory.ISBN
import io.circe.Decoder
import io.circe.generic.semiauto

final case class CreateCopyRequestBody(
  isbn: ISBN,
  condition: Condition
)

object CreateCopyRequestBody {

  implicit val decoder: Decoder[CreateCopyRequestBody] =
    semiauto.deriveDecoder

  implicit val converterTo: Converter[CreateCopyRequestBody, (domain.inventory.ISBN, domain.inventory.Condition)] =
    Converter.instance { requestBody =>
      (
        requestBody.isbn,
        requestBody.condition.convertTo[domain.inventory.Condition]
      )
    }

}

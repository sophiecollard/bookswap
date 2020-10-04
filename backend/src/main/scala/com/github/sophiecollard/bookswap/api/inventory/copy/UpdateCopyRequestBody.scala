package com.github.sophiecollard.bookswap.api.inventory.copy

import com.github.sophiecollard.bookswap.api.Converter
import com.github.sophiecollard.bookswap.api.model.inventory.Condition
import com.github.sophiecollard.bookswap.api.syntax.ConverterSyntax
import com.github.sophiecollard.bookswap.domain
import io.circe.Decoder
import io.circe.generic.semiauto

final case class UpdateCopyRequestBody(condition: Condition)

object UpdateCopyRequestBody {

  implicit val decoder: Decoder[UpdateCopyRequestBody] =
    semiauto.deriveDecoder

  implicit val converterTo: Converter[UpdateCopyRequestBody, domain.inventory.Condition] =
    Converter.instance(_.condition.convertTo[domain.inventory.Condition])

}

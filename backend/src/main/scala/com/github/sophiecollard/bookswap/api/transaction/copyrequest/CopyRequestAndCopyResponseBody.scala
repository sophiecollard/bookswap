package com.github.sophiecollard.bookswap.api.transaction.copyrequest

import com.github.sophiecollard.bookswap.api.inventory.copy.CopyResponseBody
import com.github.sophiecollard.bookswap.api.syntax.ConverterSyntax
import com.github.sophiecollard.bookswap.domain.inventory.Copy
import com.github.sophiecollard.bookswap.domain.transaction.CopyRequest
import io.circe.{Encoder, Json}
import io.circe.syntax._

final case class CopyRequestAndCopyResponseBody(
  copyRequest: CopyRequest,
  copy: Copy
)

object CopyRequestAndCopyResponseBody {

  implicit val encoder: Encoder[CopyRequestAndCopyResponseBody] =
    Encoder.instance { case CopyRequestAndCopyResponseBody(copyRequest, copy) =>
      Json.obj(
        "copyRequest" := copyRequest.convertTo[CopyRequestResponseBody].asJson,
        "copy":= copy.convertTo[CopyResponseBody].asJson
      )
    }

}

package com.github.sophiecollard.bookswap.api.transaction.copyrequest

import java.time.LocalDateTime

import com.github.sophiecollard.bookswap.api.Converter
import com.github.sophiecollard.bookswap.domain.inventory.Copy
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.{CopyRequest, RequestStatus}
import com.github.sophiecollard.bookswap.domain.user.User
import io.circe.Encoder
import io.circe.generic.semiauto

final case class CopyRequestResponseBody(
  id: Id[CopyRequest],
  copyId: Id[Copy],
  requestedBy: Id[User],
  requestedOn: LocalDateTime,
  status: RequestStatus
)

object CopyRequestResponseBody {

  implicit val encoder: Encoder[CopyRequestResponseBody] =
    semiauto.deriveEncoder

  implicit val converter: Converter[CopyRequest, CopyRequestResponseBody] =
    Converter.instance { copyRequest =>
      CopyRequestResponseBody(
        id = copyRequest.id,
        copyId = copyRequest.copyId,
        requestedBy = copyRequest.requestedBy,
        requestedOn = copyRequest.requestedOn,
        status = copyRequest.status
      )
    }

}

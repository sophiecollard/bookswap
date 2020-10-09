package com.github.sophiecollard.bookswap.api.inventory.copy

import java.time.LocalDateTime

import com.github.sophiecollard.bookswap.api.Converter
import com.github.sophiecollard.bookswap.api.model.shared.Id
import com.github.sophiecollard.bookswap.api.syntax.ConverterSyntax
import com.github.sophiecollard.bookswap.domain.inventory.{Condition, Copy, CopyStatus, ISBN}
import com.github.sophiecollard.bookswap.domain.user.User
import io.circe.Encoder
import io.circe.generic.semiauto

final case class CopyResponseBody(
  id: Id[Copy],
  isbn: ISBN,
  offeredBy: Id[User],
  offeredOn: LocalDateTime,
  condition: Condition,
  status: CopyStatus
)

object CopyResponseBody {

  implicit val encoder: Encoder[CopyResponseBody] =
    semiauto.deriveEncoder

  implicit val converter: Converter[Copy, CopyResponseBody] =
    Converter.instance { copy =>
      CopyResponseBody(
        id = copy.id.convertTo[Id[Copy]],
        isbn = copy.isbn,
        offeredBy = copy.offeredBy.convertTo[Id[User]],
        offeredOn = copy.offeredOn,
        condition = copy.condition,
        status = copy.status
      )
    }

}

package com.github.sophiecollard.bookswap.api.users

import com.github.sophiecollard.bookswap.api.Converter
import com.github.sophiecollard.bookswap.domain.user.UserStatus
import io.circe.Decoder
import io.circe.generic.semiauto

final case class UpdateUserRequestBody(status: UserStatus)

object UpdateUserRequestBody {

  implicit val decoder: Decoder[UpdateUserRequestBody] =
    semiauto.deriveDecoder

  implicit val converter: Converter[UpdateUserRequestBody, UserStatus] =
    Converter.instance(_.status)

}

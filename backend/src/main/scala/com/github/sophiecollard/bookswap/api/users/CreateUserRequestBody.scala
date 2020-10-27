package com.github.sophiecollard.bookswap.api.users

import com.github.sophiecollard.bookswap.api.Converter
import com.github.sophiecollard.bookswap.domain.shared.Name
import com.github.sophiecollard.bookswap.domain.user.User
import io.circe.Decoder
import io.circe.generic.semiauto

final case class CreateUserRequestBody(name: Name[User])

object CreateUserRequestBody {

  implicit val decoder: Decoder[CreateUserRequestBody] =
    semiauto.deriveDecoder

  implicit val converter: Converter[CreateUserRequestBody, Name[User]] =
    Converter.instance(_.name)

}

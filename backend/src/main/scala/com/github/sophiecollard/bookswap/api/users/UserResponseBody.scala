package com.github.sophiecollard.bookswap.api.users

import com.github.sophiecollard.bookswap.api.Converter
import com.github.sophiecollard.bookswap.domain.shared.{Id, Name}
import com.github.sophiecollard.bookswap.domain.user.{User, UserStatus}
import io.circe.Encoder
import io.circe.generic.semiauto

final case class UserResponseBody(
  id: Id[User],
  name: Name[User],
  status: UserStatus
)

object UserResponseBody {

  implicit val encoder: Encoder[UserResponseBody] =
    semiauto.deriveEncoder

  implicit val converter: Converter[User, UserResponseBody] =
    Converter.instance { user =>
      UserResponseBody(user.id, user.name, user.status)
    }

}

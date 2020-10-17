package com.github.sophiecollard.bookswap.api.inventory.authors

import com.github.sophiecollard.bookswap.api.Converter
import com.github.sophiecollard.bookswap.domain.inventory.Author
import com.github.sophiecollard.bookswap.domain.shared.{Id, Name}
import io.circe.Encoder
import io.circe.generic.semiauto

final case class AuthorResponseBody(
  id: Id[Author],
  name: Name[Author]
)

object AuthorResponseBody {

  implicit val encoder: Encoder[AuthorResponseBody] =
    semiauto.deriveEncoder

  implicit val converter: Converter[Author, AuthorResponseBody] =
    Converter.instance { author =>
      AuthorResponseBody(
        id = author.id,
        name = author.name
      )
    }

}

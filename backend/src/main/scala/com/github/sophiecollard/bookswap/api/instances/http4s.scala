package com.github.sophiecollard.bookswap.api.instances

import java.util.UUID

import com.github.sophiecollard.bookswap.domain.inventory.{Author, Copy, ISBN}
import com.github.sophiecollard.bookswap.domain.shared.{Id, PageSize}
import com.github.sophiecollard.bookswap.domain.transaction.CopyRequest
import com.github.sophiecollard.bookswap.domain.user.User
import org.http4s.dsl.impl.{OptionalQueryParamDecoderMatcher, QueryParamDecoderMatcher}

import scala.util.Try

trait http4s {

  object ISBNVar {
    def unapply(string: String): Option[ISBN] =
      ISBN(string)
  }

  object AuthorIdVar {
    def unapply(string: String): Option[Id[Author]] =
      Try(UUID.fromString(string))
        .map(Id.apply[Author])
        .toOption
  }

  object CopyIdVar {
    def unapply(string: String): Option[Id[Copy]] =
      Try(UUID.fromString(string))
        .map(Id.apply[Copy])
        .toOption
  }

  object CopyRequestIdVar {
    def unapply(string: String): Option[Id[CopyRequest]] =
      Try(UUID.fromString(string))
        .map(Id.apply[CopyRequest])
        .toOption
  }

  object UserIdVar {
    def unapply(string: String): Option[Id[User]] =
      Try(UUID.fromString(string))
        .map(Id.apply[User])
        .toOption
  }

  object PageSizeParamMatcher extends OptionalQueryParamDecoderMatcher[PageSize]("page_size")

  object ISBNQueryParamMatcher extends QueryParamDecoderMatcher[ISBN]("isbn")
  object CopyIdQueryParamMatcher extends QueryParamDecoderMatcher[Id[Copy]]("copy_id")

}

object http4s extends http4s

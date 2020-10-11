package com.github.sophiecollard.bookswap.api.instances

import java.util.UUID

import com.github.sophiecollard.bookswap.domain.inventory.{Copy, ISBN}
import com.github.sophiecollard.bookswap.domain.shared.{Id, PageSize}
import com.github.sophiecollard.bookswap.domain.transaction.CopyRequest
import org.http4s.dsl.impl.{OptionalQueryParamDecoderMatcher, QueryParamDecoderMatcher}

import scala.util.Try

trait http4s {

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

  object PageSizeParamMatcher extends OptionalQueryParamDecoderMatcher[PageSize]("page_size")

  object ISBNQueryParamMatcher extends QueryParamDecoderMatcher[ISBN]("isbn")
  object CopyIdQueryParamMatcher extends QueryParamDecoderMatcher[Id[Copy]]("copy_id")

}

object http4s extends http4s

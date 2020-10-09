package com.github.sophiecollard.bookswap.api.instances

import java.util.UUID

import com.github.sophiecollard.bookswap.api.model.inventory.ISBN
import com.github.sophiecollard.bookswap.api.model.shared.PageSize
import com.github.sophiecollard.bookswap.domain.inventory.Copy
import com.github.sophiecollard.bookswap.domain.shared.Id
import org.http4s.dsl.impl.{OptionalQueryParamDecoderMatcher, QueryParamDecoderMatcher}

import scala.util.Try

trait http4s {

  object CopyIdVar {
    def unapply(str: String): Option[Id[Copy]] =
      if (!str.isEmpty)
        Try(UUID.fromString(str))
          .map(Id.apply[Copy])
          .toOption
      else
        None
  }

  object PageSizeParamMatcher extends OptionalQueryParamDecoderMatcher[PageSize]("page_size")

  object ISBNQueryParamMatcher extends QueryParamDecoderMatcher[ISBN]("isbn")

}

object http4s extends http4s

package com.github.sophiecollard.bookswap.api.inventory.copy

import java.time.{LocalDateTime, ZoneId}

import com.github.sophiecollard.bookswap.api.Converter
import com.github.sophiecollard.bookswap.api.model.shared.PageSize
import com.github.sophiecollard.bookswap.api.syntax.ConverterSyntax
import com.github.sophiecollard.bookswap.domain

final case class CopyPagination(
  maybeOfferedOnOrBefore: Option[LocalDateTime],
  maybePageSize: Option[PageSize]
)

object CopyPagination {

  implicit def converter(implicit zoneId: ZoneId): Converter[CopyPagination, domain.inventory.CopyPagination] =
    Converter.instance { case CopyPagination(maybeOfferedOnOrBefore, maybePageSize) =>
      val offeredOnOrBefore = maybeOfferedOnOrBefore
        .getOrElse(LocalDateTime.now(zoneId))

      val pageSize = maybePageSize
        .map(_.convertTo[domain.shared.PageSize])
        .getOrElse(domain.shared.PageSize.default)

      domain.inventory.CopyPagination(offeredOnOrBefore, pageSize)
    }

}

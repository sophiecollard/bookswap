package com.github.sophiecollard.bookswap.domain.inventory

import java.time.{LocalDateTime, ZoneId}

import com.github.sophiecollard.bookswap.domain.shared.PageSize

final case class CopyPagination(
  offeredOnOrBefore: LocalDateTime,
  pageSize: PageSize
)

object CopyPagination {
  def default(implicit zoneId: ZoneId): CopyPagination =
    CopyPagination(
      offeredOnOrBefore = LocalDateTime.now(zoneId),
      pageSize = PageSize.ten
    )
}

package com.github.sophiecollard.bookswap.domain.inventory

import java.time.{LocalDateTime, ZoneId}

import com.github.sophiecollard.bookswap.domain.shared.PageSize

final case class CopyPagination(
  offeredOnOrBefore: LocalDateTime,
  pageSize: PageSize
)

object CopyPagination {

  def fromOptionalValues(
    maybeOfferedOnOrBefore: Option[LocalDateTime],
    maybePageSize: Option[PageSize]
  )(
    implicit zoneId: ZoneId
  ): CopyPagination =
    CopyPagination(
      offeredOnOrBefore = maybeOfferedOnOrBefore.getOrElse(defaultPageOffset),
      pageSize = maybePageSize.getOrElse(PageSize.default)
    )

  def default(implicit zoneId: ZoneId): CopyPagination =
    CopyPagination(
      offeredOnOrBefore = defaultPageOffset,
      pageSize = PageSize.default
    )

  private def defaultPageOffset(implicit zoneId: ZoneId): LocalDateTime =
    LocalDateTime.now(zoneId)

}

package com.github.sophiecollard.bookswap.domain.transaction

import java.time.{LocalDateTime, ZoneId}

import com.github.sophiecollard.bookswap.domain.shared.PageSize

final case class CopyRequestPagination(
  requestedOnOrBefore: LocalDateTime,
  pageSize: PageSize
)

object CopyRequestPagination {

  def fromOptionalValues(
    maybeRequestedOnOrBefore: Option[LocalDateTime],
    maybePageSize: Option[PageSize]
  )(
    implicit zoneId: ZoneId
  ): CopyRequestPagination =
    CopyRequestPagination(
      requestedOnOrBefore = maybeRequestedOnOrBefore.getOrElse(defaultPageOffset),
      pageSize = maybePageSize.getOrElse(PageSize.default)
    )

  def default(implicit zoneId: ZoneId): CopyRequestPagination =
    CopyRequestPagination(
      requestedOnOrBefore = defaultPageOffset,
      pageSize = PageSize.default
    )

  private def defaultPageOffset(implicit zoneId: ZoneId): LocalDateTime =
    LocalDateTime.now(zoneId)

}

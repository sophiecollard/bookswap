package com.github.sophiecollard.bookswap.syntax

import java.time.{LocalDateTime, ZoneId}

object JavaTimeSyntax {

  def now(implicit zoneId: ZoneId): LocalDateTime =
    LocalDateTime.now(zoneId)

}

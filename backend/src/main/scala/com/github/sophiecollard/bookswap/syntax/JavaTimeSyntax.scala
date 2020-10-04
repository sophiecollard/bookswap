package com.github.sophiecollard.bookswap.syntax

import java.time.{LocalDateTime, ZoneId}

trait JavaTimeSyntax {

  def now(implicit zoneId: ZoneId): LocalDateTime =
    LocalDateTime.now(zoneId)

}

object JavaTimeSyntax extends JavaTimeSyntax

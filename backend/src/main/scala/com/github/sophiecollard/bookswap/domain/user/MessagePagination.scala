package com.github.sophiecollard.bookswap.domain.user

import java.time.LocalDateTime

final case class MessagePagination(
  `from`: LocalDateTime,
  `to`: LocalDateTime
)

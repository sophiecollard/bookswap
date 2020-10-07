package com.github.sophiecollard.bookswap.domain.user

import java.time.LocalDateTime

import com.github.sophiecollard.bookswap.domain.shared.Id

final case class UserMessage(
  author: Id[User],
  createdOn: LocalDateTime,
  contents: String
)

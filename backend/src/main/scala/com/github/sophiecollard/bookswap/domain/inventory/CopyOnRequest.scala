package com.github.sophiecollard.bookswap.domain.inventory

import java.time.LocalDateTime

import cats.data.NonEmptyList
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User

final case class CopyOnRequest(
  id: Id[CopyOnRequest],
  title: Title,
  authors: NonEmptyList[Author],
  requestedBy: Id[User],
  requestedOn: LocalDateTime,
  status: CopyOnRequestStatus
)

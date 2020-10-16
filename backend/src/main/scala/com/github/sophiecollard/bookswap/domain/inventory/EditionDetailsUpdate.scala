package com.github.sophiecollard.bookswap.domain.inventory

import java.time.LocalDate

import cats.data.NonEmptyList
import com.github.sophiecollard.bookswap.api.MaybeUpdate
import com.github.sophiecollard.bookswap.domain.shared.Id

final case class EditionDetailsUpdate(
  title: MaybeUpdate[Title],
  authorIds: MaybeUpdate[NonEmptyList[Id[Author]]],
  publisherId: MaybeUpdate[Option[Id[Publisher]]],
  publicationDate: MaybeUpdate[Option[LocalDate]]
)

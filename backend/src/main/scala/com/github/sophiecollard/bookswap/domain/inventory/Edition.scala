package com.github.sophiecollard.bookswap.domain.inventory

import java.time.LocalDate

import cats.data.NonEmptyList

final case class Edition(
  isbn: ISBN,
  title: Title,
  authors: NonEmptyList[Author],
  publisher: Option[Publisher],
  publicationDate: Option[LocalDate]
)

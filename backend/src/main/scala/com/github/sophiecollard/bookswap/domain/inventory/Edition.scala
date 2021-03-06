package com.github.sophiecollard.bookswap.domain.inventory

import java.time.LocalDate

import cats.data.NonEmptyList
import com.github.sophiecollard.bookswap.domain.shared.Id

final case class Edition(
  isbn: ISBN,
  title: Title,
  authorIds: NonEmptyList[Id[Author]],
  publisherId: Option[Id[Publisher]],
  publicationDate: Option[LocalDate]
) {

  val details: EditionDetails =
    EditionDetails(title, authorIds, publisherId, publicationDate)

}

object Edition {

  def apply(isbn: ISBN, details: EditionDetails): Edition =
    Edition(
      isbn,
      title = details.title,
      authorIds = details.authorIds,
      publisherId = details.publisherId,
      publicationDate = details.publicationDate
    )

}

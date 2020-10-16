package com.github.sophiecollard.bookswap.domain.inventory

import java.time.LocalDate

import cats.data.NonEmptyList
import com.github.sophiecollard.bookswap.domain.shared.Id

final case class EditionDetails(
  title: Title,
  authorIds: NonEmptyList[Id[Author]],
  publisherId: Option[Id[Publisher]],
  publicationDate: Option[LocalDate]
) {

  def applyUpdate(update: EditionDetailsUpdate): EditionDetails =
    EditionDetails(
      title = update.title.getOrElse(this.title),
      authorIds = update.authorIds.getOrElse(this.authorIds),
      publisherId = update.publisherId.getOrElse(this.publisherId),
      publicationDate = update.publicationDate.getOrElse(this.publicationDate),
    )

}

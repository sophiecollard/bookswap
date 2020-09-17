package com.github.sophiecollard.bookswap.domain.shared

import java.util.UUID

final case class Id[A](value: UUID)

object Id {

  def generate[A]: Id[A] =
    Id(UUID.randomUUID())

}

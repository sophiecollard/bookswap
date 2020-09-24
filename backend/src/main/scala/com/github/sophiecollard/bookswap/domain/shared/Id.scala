package com.github.sophiecollard.bookswap.domain.shared

import java.util.UUID

import doobie.util.meta.Meta

final case class Id[A](value: UUID)

object Id {

  def generate[A]: Id[A] =
    Id(UUID.randomUUID())

  implicit def uuidMeta: Meta[UUID] =
    Meta[String].imap(UUID.fromString)(_.toString)

  implicit def meta[A]: Meta[Id[A]] =
    Meta[UUID].imap(apply[A])(_.value)

}

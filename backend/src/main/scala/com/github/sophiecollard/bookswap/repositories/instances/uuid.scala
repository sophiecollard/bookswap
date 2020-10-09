package com.github.sophiecollard.bookswap.repositories.instances

import java.util.UUID

import doobie.util.meta.Meta

trait uuid {

  implicit val uuidMeta: Meta[UUID] =
    Meta[String].imap(UUID.fromString)(_.toString)

}

object uuid extends uuid

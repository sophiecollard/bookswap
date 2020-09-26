package com.github.sophiecollard.bookswap.domain.inventory

import doobie.util.meta.Meta

final case class Title(value: String)

object Title {

  implicit val meta: Meta[Title] =
    Meta[String].imap(apply)(_.value)

}

package com.github.sophiecollard.bookswap.domain.inventory

import doobie.util.meta.Meta

sealed abstract case class ISBN(value: String)

object ISBN {

  def apply(value: String): Option[ISBN] = {
    val isbnPattern = "^[0-9]{13}$".r
    isbnPattern findFirstIn value map {
      new ISBN(_) {}
    }
  }

  def unvalidated(value: String): ISBN =
    new ISBN(value) {}

  implicit val meta: Meta[ISBN] =
    Meta[String].imap(unvalidated)(_.value)

}

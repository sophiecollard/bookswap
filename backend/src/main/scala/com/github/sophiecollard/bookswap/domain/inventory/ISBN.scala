package com.github.sophiecollard.bookswap.domain.inventory

sealed abstract case class ISBN(value: String)

object ISBN {

  def unvalidated(value: String): ISBN =
    new ISBN(value) {}

}

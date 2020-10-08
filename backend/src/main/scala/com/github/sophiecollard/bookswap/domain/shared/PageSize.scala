package com.github.sophiecollard.bookswap.domain.shared

sealed abstract case class PageSize(value: Int)

object PageSize {

  def apply(value: Int): Option[PageSize] =
    if (value < 0) None
    else Some(new PageSize(value) {})

  def nil: PageSize =
    new PageSize(0) {}

  def ten: PageSize =
    new PageSize(10) {}

}

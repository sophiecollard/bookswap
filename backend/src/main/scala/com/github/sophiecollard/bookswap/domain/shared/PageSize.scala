package com.github.sophiecollard.bookswap.domain.shared

sealed abstract case class PageSize(value: Int)

object PageSize {

  def apply(value: Int): Option[PageSize] =
    if (value < 0 || value > 100) None
    else Some(new PageSize(value) {})

  def default: PageSize =
    ten

  def nil: PageSize =
    new PageSize(0) {}

  def ten: PageSize =
    new PageSize(10) {}

}

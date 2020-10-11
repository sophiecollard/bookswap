package com.github.sophiecollard.bookswap.domain.shared

import org.http4s.{ParseFailure, QueryParamDecoder}

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

  implicit val queryParamDecoder: QueryParamDecoder[PageSize] =
    QueryParamDecoder[Int].emap { int =>
      apply(int) match {
        case Some(pageSize) => Right(pageSize)
        case None           => Left(ParseFailure(int.toString, s"Failed to parse PageSize from $int"))
      }
    }

}

package com.github.sophiecollard.bookswap.api.model.shared

import com.github.sophiecollard.bookswap.api.Converter
import com.github.sophiecollard.bookswap.domain
import org.http4s.{ParseFailure, QueryParamDecoder}

sealed abstract case class PageSize(value: Int)

object PageSize {

  def apply(value: Int): Option[PageSize] =
    if (value < 0 || value > 100) None
    else Some(new PageSize(value) {})

  implicit val queryParamDecoder: QueryParamDecoder[PageSize] =
    QueryParamDecoder[Int].emap { int =>
      apply(int) match {
        case Some(pageSize) => Right(pageSize)
        case None           => Left(ParseFailure(int.toString, s"Failed to parse PageSize from $int"))
      }
    }

  implicit val converterTo: Converter[PageSize, domain.shared.PageSize] =
    Converter.instance { pageSize =>
      domain.shared.PageSize(pageSize.value).get // unsafe
    }

}

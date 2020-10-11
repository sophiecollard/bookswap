package com.github.sophiecollard.bookswap.fixtures.instances

import java.time.LocalDateTime

import cats.Order

trait javatime {

  implicit val localDateTimeOrder: Order[LocalDateTime] = {
    Order.from[LocalDateTime](_ compareTo _)
  }

}

object javatime extends javatime

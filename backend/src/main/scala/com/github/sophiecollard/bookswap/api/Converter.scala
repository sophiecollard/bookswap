package com.github.sophiecollard.bookswap.api

trait Converter[A, B] {
  def convertTo(value: A): B
}

object Converter {
  def instance[A, B](f: A => B): Converter[A, B] =
    new Converter[A, B] {
      override def convertTo(value: A): B =
        f(value)
    }
}

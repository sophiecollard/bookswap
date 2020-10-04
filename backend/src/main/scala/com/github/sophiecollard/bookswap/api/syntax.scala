package com.github.sophiecollard.bookswap.api

object syntax {

  implicit class ConverterSyntax[A](val value: A) extends AnyVal {
    def convertTo[B](implicit ev: Converter[A, B]): B =
      ev.convertTo(value)
  }

}

package com.github.sophiecollard.bookswap.syntax

import cats.data.OptionT

object MonadTransformerSyntax {

  implicit class OptionTSyntax[F[_], A](value: F[Option[A]]) {
    def asOptionT: OptionT[F, A] =
      OptionT(value)
  }

}

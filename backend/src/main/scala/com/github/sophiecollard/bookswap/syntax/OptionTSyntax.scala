package com.github.sophiecollard.bookswap.syntax

import cats.Functor
import cats.data.OptionT

object OptionTSyntax {

  implicit class FOpSyntax[F[_], A](value: F[Option[A]]) {
    def asOptionT: OptionT[F, A] =
      OptionT(value)
  }

  implicit class FSyntax[F[_], A](value: F[A]) {
    def liftToOptionT(implicit ev: Functor[F]): OptionT[F, A] =
      OptionT.liftF(value)
  }

}

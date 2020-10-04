package com.github.sophiecollard.bookswap.syntax

import cats.Functor
import cats.data.OptionT

trait OptionTSyntax {

  implicit class FOpToOptionT[F[_], A](value: F[Option[A]]) {
    def asOptionT: OptionT[F, A] =
      OptionT(value)
  }

  implicit class FToOptionT[F[_], A](value: F[A]) {
    def liftToOptionT(implicit F: Functor[F]): OptionT[F, A] =
      OptionT.liftF(value)
  }

}

object OptionTSyntax extends OptionTSyntax

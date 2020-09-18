package com.github.sophiecollard.bookswap.syntax

import cats.Functor
import cats.data.{EitherT, OptionT}

object EitherTSyntax {

  implicit class FOpToEitherT[F[_], A](value: F[Option[A]]) {
    def asEitherT[E](left: => E)(implicit F: Functor[F]): EitherT[F, E, A] =
      OptionT(value).toRight(left)
  }

  implicit class FToEitherT[F[_], A](value: F[A]) {
    def liftToEitherT[E](implicit F: Functor[F]): EitherT[F, E, A] =
      EitherT.liftF(value)
  }

}

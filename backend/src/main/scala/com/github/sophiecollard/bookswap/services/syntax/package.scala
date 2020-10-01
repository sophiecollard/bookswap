package com.github.sophiecollard.bookswap.services

import cats.{Functor, ~>}
import cats.syntax.functor._

package object syntax {

  implicit class Transactable[F[_], G[_], A](private val value: G[A]) {
    def transact(transactor: G ~> F): F[A] =
      transactor(value)
  }

  implicit class FunctorOps[F[_]: Functor](private val value: F[Boolean]) {
    def mapB[A](ifTrue: => A, ifFalse: => A): F[A] =
      value.map {
        case true  => ifTrue
        case false => ifFalse
      }
  }

}

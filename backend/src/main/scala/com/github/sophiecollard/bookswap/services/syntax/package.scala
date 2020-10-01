package com.github.sophiecollard.bookswap.services

import cats.{Functor, ~>}
import cats.syntax.functor._

package object syntax {

  implicit class Transactable[F[_], G[_], A](private val value: G[A]) {
    def transact(transactor: G ~> F): F[A] =
      transactor(value)
  }

  implicit class FBooleanOps[F[_]](private val value: F[Boolean]) {
    def ifTrue[A](a: => A)(implicit ev: Functor[F]): F[Option[A]] =
      value.map {
        case true  => Some(a)
        case false => None
      }
  }

  implicit class FOptOps[F[_], A](private val value: F[Option[A]]) {
    def elseIfFalse[E](ifFalse: => E)(implicit ev: Functor[F]): F[Either[E, A]] =
      value.map {
        case None    => Left(ifFalse)
        case Some(a) => Right(a)
      }
  }

}

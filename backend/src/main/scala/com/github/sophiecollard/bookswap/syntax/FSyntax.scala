package com.github.sophiecollard.bookswap.syntax

import cats.{Functor, ~>}
import cats.syntax.functor._

trait FSyntax {

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
    def orElse[E](ifEmpty: => E)(implicit ev: Functor[F]): F[Either[E, A]] =
      value.map {
        case None    => Left(ifEmpty)
        case Some(a) => Right(a)
      }
  }

}

object FSyntax extends FSyntax
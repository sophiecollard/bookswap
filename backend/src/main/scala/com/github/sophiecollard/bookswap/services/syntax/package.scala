package com.github.sophiecollard.bookswap.services

import cats.~>

package object syntax {

  implicit class Transactable[F[_], G[_], A](value: G[A]) {
    def transact(transactor: G ~> F): F[A] =
      transactor(value)
  }

}

package com.github.sophiecollard.bookswap.fixtures.repositories.inventory

import cats.Applicative
import cats.implicits._
import com.github.sophiecollard.bookswap.domain.inventory.Author
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.repositories.inventory.AuthorsRepository

object TestAuthorsRepository {

  def create[F[_]: Applicative]: AuthorsRepository[F] =
    new AuthorsRepository[F] {
      override def get(id: Id[Author]): F[Option[Author]] =
        store.get(id).pure[F]

      override def create(author: Author): F[Boolean] =
        store.get(author.id) match {
          case Some(_) =>
            false.pure[F]
          case None =>
            store += ((author.id, author))
            true.pure[F]
        }

      override def delete(id: Id[Author]): F[Boolean] =
        store.get(id) match {
          case Some(_) =>
            store -= id
            true.pure[F]
          case None =>
            false.pure[F]
        }

      private var store: Map[Id[Author], Author] =
        Map.empty
    }

}

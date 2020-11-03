package com.github.sophiecollard.bookswap.fixtures.repositories.inventory

import cats.Applicative
import cats.implicits._
import com.github.sophiecollard.bookswap.domain.inventory.{Edition, EditionDetails, ISBN}
import com.github.sophiecollard.bookswap.repositories.inventory.EditionsRepository

object TestEditionsRepository {

  def create[F[_]: Applicative]: EditionsRepository[F] =
    new EditionsRepository[F] {
      override def create(edition: Edition): F[Boolean] =
        store.get(edition.isbn) match {
          case Some(_) =>
            false.pure[F]
          case None =>
            store += ((edition.isbn, edition))
            true.pure[F]
        }

      override def update(isbn: ISBN, details: EditionDetails): F[Boolean] =
        store.get(isbn) match {
          case Some(_) =>
            store += ((isbn, Edition(isbn, details.title, details.authorIds, details.publisherId, details.publicationDate)))
            true.pure[F]
          case None =>
            false.pure[F]
        }

      override def delete(isbn: ISBN): F[Boolean] =
        store.get(isbn) match {
          case Some(_) =>
            store -= isbn
            true.pure[F]
          case None =>
            false.pure[F]
        }

      override def get(isbn: ISBN): F[Option[Edition]] =
        store.get(isbn).pure[F]

      private var store: Map[ISBN, Edition] =
        Map.empty
    }

}

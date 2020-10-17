package com.github.sophiecollard.bookswap.fixtures.repositories.inventory

import cats.{Id => CatsId}
import com.github.sophiecollard.bookswap.domain.inventory.{Edition, EditionDetails, ISBN}
import com.github.sophiecollard.bookswap.repositories.inventory.EditionsRepository

class TestEditionsRepository extends EditionsRepository[CatsId] {

  override def create(edition: Edition): CatsId[Boolean] =
    store.get(edition.isbn) match {
      case Some(_) =>
        false
      case None =>
        store += ((edition.isbn, edition))
        true
    }

  override def update(isbn: ISBN, details: EditionDetails): CatsId[Boolean] =
    store.get(isbn) match {
      case Some(_) =>
        store += ((isbn, Edition(isbn, details.title, details.authorIds, details.publisherId, details.publicationDate)))
        true
      case None =>
        false
    }

  override def delete(isbn: ISBN): CatsId[Boolean] =
    store.get(isbn) match {
      case Some(_) =>
        store -= isbn
        true
      case None =>
        false
    }

  override def get(isbn: ISBN): CatsId[Option[Edition]] =
    store.get(isbn)

  private var store: Map[ISBN, Edition] =
    Map.empty

}

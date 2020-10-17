package com.github.sophiecollard.bookswap.fixtures.repositories.inventory

import cats.{Id => CatsId}
import com.github.sophiecollard.bookswap.domain.inventory.Author
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.repositories.inventory.AuthorsRepository

class TestAuthorsRepository extends AuthorsRepository[CatsId] {

  override def get(id: Id[Author]): CatsId[Option[Author]] =
    store.get(id)

  override def create(author: Author): CatsId[Boolean] =
    store.get(author.id) match {
      case Some(_) =>
        false
      case None =>
        store += ((author.id, author))
        true
    }

  override def delete(id: Id[Author]): CatsId[Boolean] =
    store.get(id) match {
      case Some(_) =>
        store -= id
        true
      case None =>
        false
    }

  private var store: Map[Id[Author], Author] =
    Map.empty

}

package com.github.sophiecollard.bookswap.repositories.inventory

import com.github.sophiecollard.bookswap.domain.inventory.Author
import com.github.sophiecollard.bookswap.domain.shared.Id

trait AuthorRepository[F[_]] {

  def delete(id: Id[Author]): F[Unit]

  def get(id: Id[Author]): F[Option[Author]]

}

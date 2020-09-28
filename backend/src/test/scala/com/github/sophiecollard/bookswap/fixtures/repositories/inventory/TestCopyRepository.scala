package com.github.sophiecollard.bookswap.fixtures.repositories.inventory

import cats.{Id => CatsId}
import com.github.sophiecollard.bookswap.domain.inventory.{Condition, Copy, CopyStatus}
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.repositories.inventory.CopyRepository

class TestCopyRepository extends CopyRepository[CatsId] {

  def create(copy: Copy): CatsId[Unit] =
    store += ((copy.id, copy))

  def updateCondition(id: Id[Copy], condition: Condition): CatsId[Unit] =
    store.get(id) match {
      case Some(copy) =>
        store += ((id, copy.copy(condition = condition)))
      case None =>
        ()
    }

  def updateStatus(id: Id[Copy], status: CopyStatus): CatsId[Unit] =
    store.get(id) match {
      case Some(copy) =>
        store += ((id, copy.copy(status = status)))
      case None =>
        ()
    }

  def get(id: Id[Copy]): CatsId[Option[Copy]] =
    store.get(id)

  private var store: Map[Id[Copy], Copy] =
    Map.empty

}

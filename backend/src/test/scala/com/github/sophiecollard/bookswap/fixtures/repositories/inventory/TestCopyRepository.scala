package com.github.sophiecollard.bookswap.fixtures.repositories.inventory

import cats.{Id => CatsId}
import com.github.sophiecollard.bookswap.domain.inventory.{Condition, Copy, CopyStatus}
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.repositories.inventory.CopyRepository

class TestCopyRepository extends CopyRepository[CatsId] {

  override def create(copy: Copy): CatsId[Boolean] = {
    store.get(copy.id) match {
      case Some(_) =>
        false
      case None =>
        store += ((copy.id, copy))
        true
    }
  }

  override def updateCondition(id: Id[Copy], condition: Condition): CatsId[Boolean] =
    store.get(id) match {
      case Some(copy) =>
        store += ((id, copy.copy(condition = condition)))
        true
      case None =>
        false
    }

  override def updateStatus(id: Id[Copy], status: CopyStatus): CatsId[Boolean] =
    store.get(id) match {
      case Some(copy) =>
        store += ((id, copy.copy(status = status)))
        true
      case None =>
        false
    }

  override def get(id: Id[Copy]): CatsId[Option[Copy]] =
    store.get(id)

  private var store: Map[Id[Copy], Copy] =
    Map.empty

}

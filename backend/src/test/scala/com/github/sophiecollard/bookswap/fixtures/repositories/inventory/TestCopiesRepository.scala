package com.github.sophiecollard.bookswap.fixtures.repositories.inventory

import java.time.LocalDateTime

import cats.syntax.order._
import cats.{Id => CatsId}
import com.github.sophiecollard.bookswap.domain.inventory._
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.fixtures.instances.javatime._
import com.github.sophiecollard.bookswap.repositories.inventory.CopiesRepository

class TestCopiesRepository extends CopiesRepository[CatsId] {

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

  override def listForEdition(isbn: ISBN, pagination: CopyPagination): CatsId[List[Copy]] =
    store.values.toList
      .filter { copy =>
        copy.isbn == isbn &&
          copy.status != CopyStatus.Withdrawn &&
          copy.status != CopyStatus.Swapped &&
          copy.offeredOn <= pagination.offeredOnOrBefore
      }
      .sortBy(_.offeredOn)(Ordering[LocalDateTime].reverse)
      .take(pagination.pageSize.value)

  override def listForOwner(offeredBy: Id[User], pagination: CopyPagination): CatsId[List[Copy]] =
    store.values.toList
      .filter { copy =>
        copy.offeredBy == offeredBy &&
          copy.offeredOn <= pagination.offeredOnOrBefore
      }
      .sortBy(_.offeredOn)(Ordering[LocalDateTime].reverse)
      .take(pagination.pageSize.value)

  private var store: Map[Id[Copy], Copy] =
    Map.empty

}

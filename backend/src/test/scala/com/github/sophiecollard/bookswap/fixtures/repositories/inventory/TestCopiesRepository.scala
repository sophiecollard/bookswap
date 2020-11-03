package com.github.sophiecollard.bookswap.fixtures.repositories.inventory

import java.time.LocalDateTime

import cats.Applicative
import cats.implicits._
import com.github.sophiecollard.bookswap.domain.inventory._
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.fixtures.instances.javatime._
import com.github.sophiecollard.bookswap.repositories.inventory.CopiesRepository

object TestCopiesRepository {

  def create[F[_]: Applicative]: CopiesRepository[F] =
    new CopiesRepository[F] {
      override def create(copy: Copy): F[Boolean] = {
        store.get(copy.id) match {
          case Some(_) =>
            false.pure[F]
          case None =>
            store += ((copy.id, copy))
            true.pure[F]
        }
      }

      override def updateCondition(id: Id[Copy], condition: Condition): F[Boolean] =
        store.get(id) match {
          case Some(copy) =>
            store += ((id, copy.copy(condition = condition)))
            true.pure[F]
          case None =>
            false.pure[F]
        }

      override def updateStatus(id: Id[Copy], status: CopyStatus): F[Boolean] =
        store.get(id) match {
          case Some(copy) =>
            store += ((id, copy.copy(status = status)))
            true.pure[F]
          case None =>
            false.pure[F]
        }

      override def get(id: Id[Copy]): F[Option[Copy]] =
        store.get(id).pure[F]

      override def listForEdition(isbn: ISBN, pagination: CopyPagination): F[List[Copy]] =
        store.values.toList
          .filter { copy =>
            copy.isbn == isbn &&
              copy.status != CopyStatus.Withdrawn &&
              copy.status != CopyStatus.Swapped &&
              copy.offeredOn <= pagination.offeredOnOrBefore
          }
          .sortBy(_.offeredOn)(Ordering[LocalDateTime].reverse)
          .take(pagination.pageSize.value)
          .pure[F]

      override def listForOwner(offeredBy: Id[User], pagination: CopyPagination): F[List[Copy]] =
        store.values.toList
          .filter { copy =>
            copy.offeredBy == offeredBy &&
              copy.offeredOn <= pagination.offeredOnOrBefore
          }
          .sortBy(_.offeredOn)(Ordering[LocalDateTime].reverse)
          .take(pagination.pageSize.value)
          .pure[F]

      private var store: Map[Id[Copy], Copy] =
        Map.empty
    }

}

package com.github.sophiecollard.bookswap.fixtures.repositories.transaction

import java.time.LocalDateTime

import cats.syntax.order._
import cats.{Id => CatsId}
import com.github.sophiecollard.bookswap.domain.inventory.Copy
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.RequestStatus.{Accepted, OnWaitingList, Pending}
import com.github.sophiecollard.bookswap.domain.transaction.{CopyRequest, CopyRequestPagination, RequestStatus}
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.fixtures.instances.javatime._
import com.github.sophiecollard.bookswap.repositories.transaction.CopyRequestsRepository

class TestCopyRequestsRepository extends CopyRequestsRepository[CatsId] {

  override def create(copyRequest: CopyRequest): CatsId[Boolean] = {
    store.get(copyRequest.id) match {
      case Some(_) =>
        false
      case None =>
        store += ((copyRequest.id, copyRequest))
        true
    }
  }

  override def updateStatus(id: Id[CopyRequest], newStatus: RequestStatus): CatsId[Boolean] =
    store.get(id) match {
      case Some(copyRequest) =>
        store += ((id, copyRequest.copy(status = newStatus)))
        true
      case None =>
        false
    }

  override def updatePendingRequestsStatuses(copyId: Id[Copy], newStatus: RequestStatus): CatsId[Unit] =
    store = store.map { case (id, request) =>
      request.status match {
        case Pending =>
          (id, request.copy(status = newStatus))
        case _ =>
          (id, request)
      }
    }

  override def updateAcceptedRequestsStatuses(copyId: Id[Copy], newStatus: RequestStatus): CatsId[Unit] =
    store = store.map { case (id, request) =>
      request.status match {
        case Accepted(_) =>
          (id, request.copy(status = newStatus))
        case _ =>
          (id, request)
      }
    }

  override def updateWaitingListRequestsStatuses(copyId: Id[Copy], newStatus: RequestStatus): CatsId[Unit] =
    store = store.map { case (id, request) =>
      request.status match {
        case OnWaitingList(_) =>
          (id, request.copy(status = newStatus))
        case _ =>
          (id, request)
      }
    }

  override def get(id: Id[CopyRequest]): CatsId[Option[CopyRequest]] =
    store.get(id)

  override def findFirstOnWaitingList(copyId: Id[Copy]): CatsId[Option[CopyRequest]] =
    store.values.toList
      .filter(r => r.copyId == copyId && r.status.isOnWaitingList)
      .sortBy(_.requestedOn)
      .headOption

  override def listForCopy(copyId: Id[Copy], pagination: CopyRequestPagination): CatsId[List[CopyRequest]] =
    store.values.toList
      .filter { request =>
        request.copyId == copyId &&
          request.requestedOn <= pagination.requestedOnOrBefore
      }
      .sortBy(_.requestedOn)(Ordering[LocalDateTime].reverse)
      .take(pagination.pageSize.value)

  override def listForRequester(requestedBy: Id[User], pagination: CopyRequestPagination): CatsId[List[CopyRequest]] =
    store.values.toList
      .filter { request =>
        request.requestedBy == requestedBy &&
          request.requestedOn <= pagination.requestedOnOrBefore
      }
      .sortBy(_.requestedOn)(Ordering[LocalDateTime].reverse)
      .take(pagination.pageSize.value)

  private var store: Map[Id[CopyRequest], CopyRequest] =
    Map.empty

}

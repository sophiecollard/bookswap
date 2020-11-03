package com.github.sophiecollard.bookswap.fixtures.repositories.transaction

import java.time.LocalDateTime

import cats.Applicative
import cats.implicits._
import com.github.sophiecollard.bookswap.domain.inventory.Copy
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.RequestStatus.{Accepted, OnWaitingList, Pending}
import com.github.sophiecollard.bookswap.domain.transaction.{CopyRequest, CopyRequestPagination, RequestStatus}
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.fixtures.instances.javatime._
import com.github.sophiecollard.bookswap.repositories.transaction.CopyRequestsRepository

object TestCopyRequestsRepository {

  def create[F[_]: Applicative]: CopyRequestsRepository[F] =
    new CopyRequestsRepository[F] {
      override def create(copyRequest: CopyRequest): F[Boolean] = {
        store.get(copyRequest.id) match {
          case Some(_) =>
            false.pure[F]
          case None =>
            store += ((copyRequest.id, copyRequest))
            true.pure[F]
        }
      }

      override def updateStatus(id: Id[CopyRequest], newStatus: RequestStatus): F[Boolean] =
        store.get(id) match {
          case Some(copyRequest) =>
            store += ((id, copyRequest.copy(status = newStatus)))
            true.pure[F]
          case None =>
            false.pure[F]
        }

      override def updatePendingRequestsStatuses(copyId: Id[Copy], newStatus: RequestStatus): F[Unit] = {
        store = store.map { case (id, request) =>
          request.status match {
            case Pending =>
              (id, request.copy(status = newStatus))
            case _ =>
              (id, request)
          }
        }
      }.pure[F]

      override def updateAcceptedRequestsStatuses(copyId: Id[Copy], newStatus: RequestStatus): F[Unit] = {
        store = store.map { case (id, request) =>
          request.status match {
            case Accepted(_) =>
              (id, request.copy(status = newStatus))
            case _ =>
              (id, request)
          }
        }
      }.pure[F]

      override def updateWaitingListRequestsStatuses(copyId: Id[Copy], newStatus: RequestStatus): F[Unit] = {
        store = store.map { case (id, request) =>
          request.status match {
            case OnWaitingList(_) =>
              (id, request.copy(status = newStatus))
            case _ =>
              (id, request)
          }
        }
      }.pure[F]

      override def get(id: Id[CopyRequest]): F[Option[CopyRequest]] =
        store.get(id).pure[F]

      override def findFirstOnWaitingList(copyId: Id[Copy]): F[Option[CopyRequest]] =
        store.values.toList
          .filter(r => r.copyId == copyId && r.status.isOnWaitingList)
          .sortBy(_.requestedOn)
          .headOption
          .pure[F]

      override def listForCopy(copyId: Id[Copy], pagination: CopyRequestPagination): F[List[CopyRequest]] =
        store.values.toList
          .filter { request =>
            request.copyId == copyId &&
              request.requestedOn <= pagination.requestedOnOrBefore
          }
          .sortBy(_.requestedOn)(Ordering[LocalDateTime].reverse)
          .take(pagination.pageSize.value)
          .pure[F]

      override def listForRequester(requestedBy: Id[User], pagination: CopyRequestPagination): F[List[CopyRequest]] =
        store.values.toList
          .filter { request =>
            request.requestedBy == requestedBy &&
              request.requestedOn <= pagination.requestedOnOrBefore
          }
          .sortBy(_.requestedOn)(Ordering[LocalDateTime].reverse)
          .take(pagination.pageSize.value)
          .pure[F]

      private var store: Map[Id[CopyRequest], CopyRequest] =
        Map.empty
    }

}

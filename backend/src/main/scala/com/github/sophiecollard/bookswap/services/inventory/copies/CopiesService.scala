package com.github.sophiecollard.bookswap.services.inventory.copies

import java.time.ZoneId

import cats.implicits._
import cats.{Monad, ~>}
import com.github.sophiecollard.bookswap.authorization.AuthorizationService
import com.github.sophiecollard.bookswap.authorization.instances.{ByActiveStatus, WithAuthorizationByActiveStatus}
import com.github.sophiecollard.bookswap.domain.inventory.CopyStatus.{Available, Withdrawn}
import com.github.sophiecollard.bookswap.domain.inventory._
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.repositories.inventory.CopiesRepository
import com.github.sophiecollard.bookswap.repositories.transaction.CopyRequestsRepository
import com.github.sophiecollard.bookswap.services.error.ServiceError._
import com.github.sophiecollard.bookswap.services.error.{ServiceError, ServiceErrorOr}
import com.github.sophiecollard.bookswap.services.inventory.copies.authorization._
import com.github.sophiecollard.bookswap.services.inventory.copies.state.StateUpdate._
import com.github.sophiecollard.bookswap.services.inventory.copies.state.{InitialState, StateMachine, StateUpdate}
import com.github.sophiecollard.bookswap.syntax._

trait CopiesService[F[_]] {

  /** Fetches a Copy */
  def get(id: Id[Copy]): F[ServiceErrorOr[Copy]]

  /** Fetches a list of Copies for the specified ISBN */
  def listForEdition(isbn: ISBN, pagination: CopyPagination): F[List[Copy]]

  /** Fetches a list of Copies offered by a User */
  def listForOwner(offeredBy: Id[User], pagination: CopyPagination): F[List[Copy]]

  /** Invoked by a registered user to create a new Copy */
  def create(edition: ISBN, condition: Condition)(userId: Id[User]): F[WithAuthorizationByActiveStatus[ServiceErrorOr[Copy]]]

  /** Invoked by the Copy owner to update its condition */
  def updateCondition(id: Id[Copy], condition: Condition)(userId: Id[User]): F[WithAuthorizationByCopyOwner[ServiceErrorOr[Copy]]]

  /** Invoked by the Copy owner to withdraw that Copy */
  def withdraw(id: Id[Copy])(userId: Id[User]): F[WithAuthorizationByCopyOwner[ServiceErrorOr[Copy]]]

}

object CopiesService {

  def create[F[_]: Monad, G[_]: Monad](
    authorizationByActiveStatus: AuthorizationService[F, Id[User], ByActiveStatus],
    authorizationByCopyOwner: AuthorizationService[F, (Id[User], Id[Copy]), ByCopyOwner],
    copiesRepository: CopiesRepository[G],
    copyRequestsRepository: CopyRequestsRepository[G],
    transactor: G ~> F
  )(
    implicit zoneId: ZoneId
  ): CopiesService[F] = new CopiesService[F] {
    override def get(id: Id[Copy]): F[ServiceErrorOr[Copy]] =
      getWithoutTransaction(id)
        .transact(transactor)

    override def listForEdition(isbn: ISBN, pagination: CopyPagination): F[List[Copy]] =
      copiesRepository
        .listForEdition(isbn, pagination)
        .transact(transactor)

    override def listForOwner(offeredBy: Id[User], pagination: CopyPagination): F[List[Copy]] =
      copiesRepository
        .listForOwner(offeredBy, pagination)
        .transact(transactor)

    override def create(edition: ISBN, condition: Condition)(userId: Id[User]): F[WithAuthorizationByActiveStatus[ServiceErrorOr[Copy]]] =
      authorizationByActiveStatus.authorize(userId) {
        val copy = Copy(
          id = Id.generate[Copy],
          edition,
          offeredBy = userId,
          offeredOn = now,
          condition,
          status = Available
        )

        copiesRepository
          .create(copy)
          .ifTrue(copy)
          .orElse[ServiceError](FailedToCreateResource("Copy", copy.id))
          .transact(transactor)
      }

    override def updateCondition(id: Id[Copy], condition: Condition)(userId: Id[User]): F[WithAuthorizationByCopyOwner[ServiceErrorOr[Copy]]] =
      authorizationByCopyOwner.authorize((userId, id)) {
        val result = for {
          copy <- getWithoutTransaction(id).asEitherT
          updatedCopy <- copiesRepository
            .updateCondition(id, condition)
            .ifTrue(copy.copy(condition = condition))
            .orElse[ServiceError](FailedToUpdateResource("Copy", id))
            .asEitherT
        } yield updatedCopy

        result.value.transact(transactor)
      }

    override def withdraw(id: Id[Copy])(userId: Id[User]): F[WithAuthorizationByCopyOwner[ServiceErrorOr[Copy]]] =
      authorizationByCopyOwner.authorize((userId, id)) {
        val result = for {
          copy <- getWithoutTransaction(id).asEitherT
          initialState = InitialState(copy.status)
          stateUpdate = StateMachine.handleWithdrawCommand(initialState)
          updatedStatus <- performStateUpdate(id, initialState)(stateUpdate).asEitherT
          updatedCopy = copy.copy(status = updatedStatus)
        } yield updatedCopy

        result.value.transact(transactor)
      }

    private def getWithoutTransaction(id: Id[Copy]): G[ServiceErrorOr[Copy]] =
      copiesRepository
        .get(id)
        .orElse[ServiceError](ResourceNotFound("Copy", id))

    private def performStateUpdate(
      copyId: Id[Copy],
      initialState: InitialState
    )(
      stateUpdate: StateUpdate
    ): G[ServiceErrorOr[CopyStatus]] =
      stateUpdate match {
        case UpdateCopyAndOpenRequestsStatuses(copyStatus, openRequestsStatus) =>
          copyRequestsRepository.updatePendingRequestsStatuses(copyId, openRequestsStatus) >>
            copyRequestsRepository.updateAcceptedRequestsStatuses(copyId, openRequestsStatus) >>
            copyRequestsRepository.updateWaitingListRequestsStatuses(copyId, openRequestsStatus) >>
            copiesRepository.updateStatus(copyId, Withdrawn) ifTrue
            copyStatus orElse[ServiceError]
            FailedToUpdateResource("Copy", copyId)
        case NoUpdate =>
          initialState.copyStatus.asRight[ServiceError].pure[G]
      }
  }

}

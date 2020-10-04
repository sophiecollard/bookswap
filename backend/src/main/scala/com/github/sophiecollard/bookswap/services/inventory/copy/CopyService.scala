package com.github.sophiecollard.bookswap.services.inventory.copy

import java.time.ZoneId

import cats.implicits._
import cats.{Monad, ~>}
import com.github.sophiecollard.bookswap.domain.inventory.CopyStatus.{Available, Withdrawn}
import com.github.sophiecollard.bookswap.domain.inventory.{Condition, Copy, CopyStatus, ISBN}
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.error.ServiceError._
import com.github.sophiecollard.bookswap.error.{ServiceError, ServiceErrorOr}
import com.github.sophiecollard.bookswap.repositories.inventory.CopyRepository
import com.github.sophiecollard.bookswap.repositories.transaction.CopyRequestRepository
import com.github.sophiecollard.bookswap.services.authorization.AuthorizationService
import com.github.sophiecollard.bookswap.services.authorization.Instances.{ByActiveStatus, WithAuthorizationByActiveStatus}
import com.github.sophiecollard.bookswap.services.inventory.copy.Authorization._
import com.github.sophiecollard.bookswap.services.inventory.copy.state.StateUpdate._
import com.github.sophiecollard.bookswap.services.inventory.copy.state.{InitialState, StateMachine, StateUpdate}
import com.github.sophiecollard.bookswap.syntax._

trait CopyService[F[_]] {

  /** Fetches a Copy */
  def get(id: Id[Copy]): F[ServiceErrorOr[Copy]]

  /** Invoked by a registered user to create a new Copy */
  def create(edition: ISBN, condition: Condition)(userId: Id[User]): F[WithAuthorizationByActiveStatus[ServiceErrorOr[Copy]]]

  /** Invoked by the Copy owner to update its condition */
  def updateCondition(id: Id[Copy], condition: Condition)(userId: Id[User]): F[WithAuthorizationByCopyOwner[ServiceErrorOr[Condition]]]

  /** Invoked by the Copy owner to withdraw that Copy */
  def withdraw(id: Id[Copy])(userId: Id[User]): F[WithAuthorizationByCopyOwner[ServiceErrorOr[CopyStatus]]]

}

object CopyService {

  def create[F[_]: Monad, G[_]: Monad](
    authorizationByActiveStatus: AuthorizationService[F, Id[User], ByActiveStatus],
    authorizationByCopyOwner: AuthorizationService[F, AuthorizationInput, ByCopyOwner],
    copyRepository: CopyRepository[G],
    copyRequestRepository: CopyRequestRepository[G],
    transactor: G ~> F
  )(
    implicit zoneId: ZoneId
  ): CopyService[F] = new CopyService[F] {
    override def get(id: Id[Copy]): F[ServiceErrorOr[Copy]] =
      copyRepository
        .get(id)
        .orElse[ServiceError](ResourceNotFound("Copy", id))
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

        copyRepository
          .create(copy)
          .ifTrue(copy)
          .orElse[ServiceError](FailedToCreateResource("Copy", copy.id))
          .transact(transactor)
      }

    override def updateCondition(id: Id[Copy], condition: Condition)(userId: Id[User]): F[WithAuthorizationByCopyOwner[ServiceErrorOr[Condition]]] =
      authorizationByCopyOwner.authorize(AuthorizationInput(userId, id)) {
        copyRepository
          .updateCondition(id, condition)
          .ifTrue(condition)
          .orElse[ServiceError](FailedToUpdateResource("Copy", id))
          .transact(transactor)
      }

    override def withdraw(id: Id[Copy])(userId: Id[User]): F[WithAuthorizationByCopyOwner[ServiceErrorOr[CopyStatus]]] =
      authorizationByCopyOwner.authorize(AuthorizationInput(userId, id)) {
        copyRepository
          .get(id)
          .transact(transactor)
          .asEitherT[ServiceError](ResourceNotFound("Copy", id))
          .flatMapF { copy =>
            val initialState = InitialState(copy.status)
            val stateUpdate = StateMachine.handleWithdrawCommand(initialState)
            performStateUpdate(id, initialState)(stateUpdate)
          }
          .value
      }

    private def performStateUpdate(
      copyId: Id[Copy],
      initialState: InitialState
    )(
      stateUpdate: StateUpdate
    ): F[Either[ServiceError, CopyStatus]] =
      (
        stateUpdate match {
          case UpdateCopyAndOpenRequestsStatuses(copyStatus, openRequestsStatus) =>
            copyRequestRepository.updatePendingRequestsStatuses(copyId, openRequestsStatus) >>
              copyRequestRepository.updateAcceptedRequestsStatuses(copyId, openRequestsStatus) >>
              copyRequestRepository.updateWaitingListRequestsStatuses(copyId, openRequestsStatus) >>
              copyRepository.updateStatus(copyId, Withdrawn) ifTrue
              copyStatus orElse[ServiceError]
              FailedToUpdateResource("Copy", copyId)
          case NoUpdate =>
            initialState.copyStatus.asRight[ServiceError].pure[G]
        }
      ).transact(transactor)
  }

}

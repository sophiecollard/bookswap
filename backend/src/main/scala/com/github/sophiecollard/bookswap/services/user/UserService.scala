package com.github.sophiecollard.bookswap.services.user

import cats.{Monad, ~>}
import com.github.sophiecollard.bookswap.authorization.AuthorizationService
import com.github.sophiecollard.bookswap.authorization.instances.{ByAdminStatus, WithAuthorizationByAdminStatus}
import com.github.sophiecollard.bookswap.domain.shared.{Id, Name}
import com.github.sophiecollard.bookswap.domain.user.{User, UserStatus}
import com.github.sophiecollard.bookswap.repositories.user.UsersRepository
import com.github.sophiecollard.bookswap.services.error.ServiceError.{FailedToCreateResource, FailedToDeleteResource, FailedToUpdateResource, ResourceNotFound, UserNameAlreadyTaken}
import com.github.sophiecollard.bookswap.services.error.{ServiceError, ServiceErrorOr}
import com.github.sophiecollard.bookswap.syntax._

trait UserService[F[_]] {

  /** Fetches a User */
  def get(id: Id[User]): F[ServiceErrorOr[User]]

  /** Creates a new User */
  def create(name: Name[User]): F[ServiceErrorOr[User]]

  /** Invoked by an Admin to update a User's status */
  def updateStatus(id: Id[User], status: UserStatus)(userId: Id[User]): F[WithAuthorizationByAdminStatus[ServiceErrorOr[Unit]]]

  /** Invoked by a User to delete itself */
  def delete(userId: Id[User]): F[ServiceErrorOr[Unit]]

}

object UserService {

  def create[F[_], G[_]: Monad](
    authorizationByAdminStatus: AuthorizationService[F, Id[User], ByAdminStatus],
    usersRepository: UsersRepository[G],
    transactor: G ~> F
  ): UserService[F] = new UserService[F] {
    override def get(id: Id[User]): F[ServiceErrorOr[User]] =
      getWithoutTransaction(id)
        .transact(transactor)

    override def create(name: Name[User]): F[ServiceErrorOr[User]] = {
      val user = User(id = Id.generate[User], name, status = UserStatus.PendingVerification)

      val result = for {
        _ <- usersRepository
          .getByName(name)
          .emptyOrElse[ServiceError](UserNameAlreadyTaken(name))
          .asEitherT
        _ <- usersRepository
          .create(user)
          .ifTrue(user)
          .orElse[ServiceError](FailedToCreateResource("User", user.id))
          .asEitherT
      } yield user

      result.value.transact(transactor)
    }

    override def updateStatus(id: Id[User], status: UserStatus)(userId: Id[User]): F[WithAuthorizationByAdminStatus[ServiceErrorOr[Unit]]] =
      authorizationByAdminStatus.authorize(userId) {
        val result = for {
          _ <- getWithoutTransaction(id).asEitherT
          _ <- usersRepository
            .updateStatus(id, status)
            .ifTrue(())
            .orElse[ServiceError](FailedToUpdateResource("User", id))
            .asEitherT
        } yield ()

        result.value.transact(transactor)
      }

    // TODO withdraw all copies and cancel all copy requests by that user
    override def delete(userId: Id[User]): F[ServiceErrorOr[Unit]] = {
      val result = for {
        _ <- getWithoutTransaction(userId).asEitherT
        _ <- usersRepository
          .delete(userId)
          .ifTrue(())
          .orElse[ServiceError](FailedToDeleteResource("User", userId))
          .asEitherT
      } yield ()

      result.value.transact(transactor)
    }

    private def getWithoutTransaction(id: Id[User]): G[ServiceErrorOr[User]] =
      usersRepository
        .get(id)
        .orElse[ServiceError](ResourceNotFound("User", id))
  }

}

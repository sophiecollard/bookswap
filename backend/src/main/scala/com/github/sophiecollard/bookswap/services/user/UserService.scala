package com.github.sophiecollard.bookswap.services.user

import cats.{Monad, ~>}
import com.github.sophiecollard.bookswap.authorization.AuthorizationService
import com.github.sophiecollard.bookswap.authorization.instances.{BySelf, WithAuthorizationBySelf}
import com.github.sophiecollard.bookswap.domain.shared.{Id, Name}
import com.github.sophiecollard.bookswap.domain.user.{User, UserStatus}
import com.github.sophiecollard.bookswap.repositories.user.UserRepository
import com.github.sophiecollard.bookswap.services.error.ServiceError.{FailedToCreateResource, FailedToDeleteResource, ResourceNotFound, UserNameAlreadyTaken}
import com.github.sophiecollard.bookswap.services.error.{ServiceError, ServiceErrorOr}
import com.github.sophiecollard.bookswap.syntax._

trait UserService[F[_]] {

  /** Fetches a User */
  def get(id: Id[User]): F[ServiceErrorOr[User]]

  /** Creates a new User */
  def create(name: Name[User]): F[ServiceErrorOr[User]]

  /** Invoked by a User to delete itself */
  def delete(id: Id[User])(userId: Id[User]): F[WithAuthorizationBySelf[ServiceErrorOr[Unit]]]

}

object UserService {

  def create[F[_], G[_]: Monad](
    authorizationBySelf: AuthorizationService[F, (Id[User], Id[User]), BySelf],
    userRepository: UserRepository[G],
    transactor: G ~> F
  ): UserService[F] = new UserService[F] {
    override def get(id: Id[User]): F[ServiceErrorOr[User]] =
      getWithoutTransaction(id)
        .transact(transactor)

    override def create(name: Name[User]): F[ServiceErrorOr[User]] = {
      val user = User(id = Id.generate[User], name, status = UserStatus.PendingVerification)

      val result = for {
        _ <- userRepository
          .getByName(name)
          .emptyOrElse[ServiceError](UserNameAlreadyTaken(name))
          .asEitherT
        _ <- userRepository
          .create(user)
          .ifTrue(user)
          .orElse[ServiceError](FailedToCreateResource("User", user.id))
          .asEitherT
      } yield user

      result.value.transact(transactor)
    }

    override def delete(id: Id[User])(userId: Id[User]): F[WithAuthorizationBySelf[ServiceErrorOr[Unit]]] =
      authorizationBySelf.authorize((id, userId)) {
        val result = for {
          _ <- getWithoutTransaction(id).asEitherT
          _ <- userRepository
            .delete(id)
            .ifTrue(())
            .orElse[ServiceError](FailedToDeleteResource("User", id))
            .asEitherT
        } yield ()

        result.value.transact(transactor)
      }

    private def getWithoutTransaction(id: Id[User]): G[ServiceErrorOr[User]] =
      userRepository
        .get(id)
        .orElse[ServiceError](ResourceNotFound("User", id))
  }

}

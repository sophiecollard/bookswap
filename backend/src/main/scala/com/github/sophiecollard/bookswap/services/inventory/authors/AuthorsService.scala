package com.github.sophiecollard.bookswap.services.inventory.authors

import cats.{Monad, ~>}
import com.github.sophiecollard.bookswap.authorization._
import com.github.sophiecollard.bookswap.authorization.instances._
import com.github.sophiecollard.bookswap.domain.inventory.Author
import com.github.sophiecollard.bookswap.domain.shared.{Id, Name}
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.repositories.inventory.AuthorsRepository
import com.github.sophiecollard.bookswap.services.error.ServiceError.{FailedToCreateResource, FailedToDeleteResource, ResourceNotFound}
import com.github.sophiecollard.bookswap.services.error.{ServiceError, ServiceErrorOr}
import com.github.sophiecollard.bookswap.syntax._

trait AuthorsService[F[_]] {

  /** Fetches an Author */
  def get(id: Id[Author]): F[ServiceErrorOr[Author]]

  /** Invoked by a registered user to create an Author */
  def create(name: Name[Author])(userId: Id[User]): F[WithAuthorizationByActiveStatus[ServiceErrorOr[Author]]]

  /** Invoked by an admin user to delete an Author */
  def delete(id: Id[Author])(userId: Id[User]): F[WithAuthorizationByAdminStatus[ServiceErrorOr[Unit]]]

}

object AuthorsService {

  def create[F[_], G[_]: Monad](
    authorizationByActiveStatus: AuthorizationService[F, Id[User], ByActiveStatus],
    authorizationByAdminStatus: AuthorizationService[F, Id[User], ByAdminStatus],
    repository: AuthorsRepository[G],
    transactor: G ~> F
  ): AuthorsService[F] =
    new AuthorsService[F] {
      override def get(id: Id[Author]): F[ServiceErrorOr[Author]] =
        getWithoutTransaction(id)
          .transact(transactor)

      override def create(name: Name[Author])(userId: Id[User]): F[WithAuthorizationByActiveStatus[ServiceErrorOr[Author]]] =
        authorizationByActiveStatus.authorize(userId) {
          val author = Author(id = Id.generate[Author], name)

          repository
            .create(author)
            .ifTrue(author)
            .orElse[ServiceError](FailedToCreateResource("Author", author.id))
            .transact(transactor)
        }

      override def delete(id: Id[Author])(userId: Id[User]): F[WithAuthorizationByAdminStatus[ServiceErrorOr[Unit]]] =
        authorizationByAdminStatus.authorize(userId) {
          val result = for {
            _ <- getWithoutTransaction(id).asEitherT
            _ <- repository
              .delete(id)
              .ifTrue(())
              .orElse[ServiceError](FailedToDeleteResource("Author", id))
              .asEitherT
          } yield ()

          result.value.transact(transactor)
        }

      private def getWithoutTransaction(id: Id[Author]): G[ServiceErrorOr[Author]] =
        repository
          .get(id)
          .orElse[ServiceError](ResourceNotFound("Author", id))
    }

}

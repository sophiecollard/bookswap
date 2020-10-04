package com.github.sophiecollard.bookswap.services.inventory.author

import cats.{Functor, ~>}
import com.github.sophiecollard.bookswap.domain.inventory.Author
import com.github.sophiecollard.bookswap.domain.shared.{Id, Name}
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.error.ServiceError.{FailedToCreateResource, FailedToDeleteResource, ResourceNotFound}
import com.github.sophiecollard.bookswap.error.{ServiceError, ServiceErrorOr}
import com.github.sophiecollard.bookswap.repositories.inventory.AuthorRepository
import com.github.sophiecollard.bookswap.services.authorization.Instances._
import com.github.sophiecollard.bookswap.services.authorization._
import com.github.sophiecollard.bookswap.syntax._

trait AuthorService[F[_]] {

  /** Fetches an Author */
  def get(id: Id[Author]): F[ServiceErrorOr[Author]]

  /** Invoked by a registered user to create an Author */
  def create(name: Name[Author])(userId: Id[User]): F[WithAuthorizationByActiveStatus[ServiceErrorOr[Author]]]

  /** Invoked by an admin user to delete an Author */
  def delete(id: Id[Author])(userId: Id[User]): F[WithAuthorizationByAdminStatus[ServiceErrorOr[Unit]]]

}

object AuthorService {

  def create[F[_], G[_]: Functor](
    authorizationByActiveStatus: AuthorizationService[F, Id[User], ByActiveStatus],
    authorizationByAdminStatus: AuthorizationService[F, Id[User], ByAdminStatus],
    repository: AuthorRepository[G],
    transactor: G ~> F
  ): AuthorService[F] =
    new AuthorService[F] {
      override def get(id: Id[Author]): F[ServiceErrorOr[Author]] =
        repository
          .get(id)
          .orElse[ServiceError](ResourceNotFound("Author", id))
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
          repository
            .delete(id)
            .ifTrue(())
            .orElse[ServiceError](FailedToDeleteResource("Author", id))
            .transact(transactor)
        }
    }

}

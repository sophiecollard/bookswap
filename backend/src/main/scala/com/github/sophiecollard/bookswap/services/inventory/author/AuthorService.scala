package com.github.sophiecollard.bookswap.services.inventory.author

import cats.{Functor, ~>}
import cats.implicits._
import com.github.sophiecollard.bookswap.domain.inventory.Author
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.error.Error.ServiceError.{FailedToDeleteResource, ResourceNotFound}
import com.github.sophiecollard.bookswap.error.Error.{ServiceError, ServiceErrorOr}
import com.github.sophiecollard.bookswap.repositories.inventory.AuthorRepository
import com.github.sophiecollard.bookswap.services.authorization.Instances._
import com.github.sophiecollard.bookswap.services.authorization._
import com.github.sophiecollard.bookswap.services.syntax._

trait AuthorService[F[_]] {

  def get(id: Id[Author]): F[ServiceErrorOr[Author]]

  def delete(id: Id[Author])(userId: Id[User]): F[WithAuthorizationByAdminStatus[ServiceErrorOr[Unit]]]

}

object AuthorService {

  def create[F[_], G[_]: Functor](
    authorizationService: AuthorizationService[F, Id[User], ByAdminStatus],
    repository: AuthorRepository[G],
    transactor: G ~> F
  ): AuthorService[F] =
    new AuthorService[F] {
      override def get(id: Id[Author]): F[ServiceErrorOr[Author]] =
        repository
          .get(id)
          .map(_.toRight[ServiceError](ResourceNotFound("Author", id)))
          .transact(transactor)

      override def delete(id: Id[Author])(userId: Id[User]): F[WithAuthorizationByAdminStatus[ServiceErrorOr[Unit]]] =
        authorizationService.authorize(userId) {
          repository
            .delete(id)
            .mapB[Either[ServiceError, Unit]](Right(()), Left(FailedToDeleteResource("Author", id)))
            .transact(transactor)
        }
    }

}

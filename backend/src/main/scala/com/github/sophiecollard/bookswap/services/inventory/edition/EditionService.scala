package com.github.sophiecollard.bookswap.services.inventory.edition

import cats.{Functor, ~>}
import cats.implicits._
import com.github.sophiecollard.bookswap.domain.inventory.{Edition, ISBN}
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.error.Error.ServiceError.{EditionNotFound, FailedToDeleteEdition}
import com.github.sophiecollard.bookswap.error.Error.{ServiceError, ServiceErrorOr}
import com.github.sophiecollard.bookswap.repositories.inventory.EditionRepository
import com.github.sophiecollard.bookswap.services.authorization.AuthorizationService
import com.github.sophiecollard.bookswap.services.authorization.Instances._
import com.github.sophiecollard.bookswap.services.syntax._

trait EditionService[F[_]] {

  def get(isbn: ISBN): F[ServiceErrorOr[Edition]]

  def delete(isbn: ISBN)(userId: Id[User]): F[WithAuthorizationByAdminStatus[ServiceErrorOr[Unit]]]

}

object EditionService {

  def create[F[_], G[_]: Functor](
    authorizationByAdminStatus: AuthorizationService[F, Id[User], ByAdminStatus],
    repository: EditionRepository[G],
    transactor: G ~> F
  ): EditionService[F] = new EditionService[F] {
    override def get(isbn: ISBN): F[ServiceErrorOr[Edition]] =
      repository
        .get(isbn)
        .map(_.toRight[ServiceError](EditionNotFound(isbn)))
        .transact(transactor)

    // TODO Check that there are no open Copy offers for this edition
    override def delete(isbn: ISBN)(userId: Id[User]): F[WithAuthorizationByAdminStatus[ServiceErrorOr[Unit]]] =
      authorizationByAdminStatus.authorize(userId) {
        repository
          .delete(isbn)
          .mapB[Either[ServiceError, Unit]](Right(()), Left(FailedToDeleteEdition(isbn)))
          .transact(transactor)
      }
  }

}

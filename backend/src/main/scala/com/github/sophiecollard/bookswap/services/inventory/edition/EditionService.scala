package com.github.sophiecollard.bookswap.services.inventory.edition

import cats.{Functor, ~>}
import cats.implicits._
import com.github.sophiecollard.bookswap.domain.inventory.{Edition, ISBN}
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.error.Error.ServiceError.EditionNotFound
import com.github.sophiecollard.bookswap.error.Error.{ServiceError, ServiceErrorOr}
import com.github.sophiecollard.bookswap.repositories.inventory.EditionRepository
import com.github.sophiecollard.bookswap.services.authorization.AuthorizationService
import com.github.sophiecollard.bookswap.services.authorization.Instances._
import com.github.sophiecollard.bookswap.services.syntax._

trait EditionService[F[_]] {

  def get(isbn: ISBN): F[ServiceErrorOr[Edition]]

  def delete(isbn: ISBN)(userId: Id[User]): F[WithAuthorizationByAdminStatus[Unit]]

}

object EditionService {

  def create[F[_], G[_]: Functor](
    authorizationService: AuthorizationService[F, Id[User], ByAdminStatus],
    repository: EditionRepository[G],
    transactor: G ~> F
  ): EditionService[F] = new EditionService[F] {
    override def get(isbn: ISBN): F[ServiceErrorOr[Edition]] =
      repository
        .get(isbn)
        .map(_.toRight[ServiceError](EditionNotFound(isbn)))
        .transact(transactor)

    override def delete(isbn: ISBN)(userId: Id[User]): F[WithAuthorizationByAdminStatus[Unit]] =
      authorizationService.authorize(userId) {
        repository.delete(isbn).transact(transactor)
      }
  }

}

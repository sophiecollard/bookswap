package com.github.sophiecollard.bookswap.services.inventory.edition

import cats.{Functor, ~>}
import com.github.sophiecollard.bookswap.authorization.AuthorizationService
import com.github.sophiecollard.bookswap.authorization.instances._
import com.github.sophiecollard.bookswap.domain.inventory.{Edition, EditionDetails, ISBN}
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.error.ServiceError._
import com.github.sophiecollard.bookswap.error.{ServiceError, ServiceErrorOr}
import com.github.sophiecollard.bookswap.repositories.inventory.EditionRepository
import com.github.sophiecollard.bookswap.syntax._

trait EditionService[F[_]] {

  /** Fetches an Edition */
  def get(isbn: ISBN): F[ServiceErrorOr[Edition]]

  /** Invoked by a registered user to create an Edition */
  def create(edition: Edition)(userId: Id[User]): F[WithAuthorizationByActiveStatus[ServiceErrorOr[Edition]]]

  /** Invoked by a registered user to update an Edition */
  def update(isbn: ISBN, details: EditionDetails)(userId: Id[User]): F[WithAuthorizationByActiveStatus[ServiceErrorOr[Edition]]]

  /** Invoked by an admin user to delete an Edition */
  def delete(isbn: ISBN)(userId: Id[User]): F[WithAuthorizationByAdminStatus[ServiceErrorOr[Unit]]]

}

object EditionService {

  def create[F[_], G[_]: Functor](
    authorizationByActiveStatus: AuthorizationService[F, Id[User], ByActiveStatus],
    authorizationByAdminStatus: AuthorizationService[F, Id[User], ByAdminStatus],
    repository: EditionRepository[G],
    transactor: G ~> F
  ): EditionService[F] = new EditionService[F] {
    override def get(isbn: ISBN): F[ServiceErrorOr[Edition]] =
      repository
        .get(isbn)
        .orElse[ServiceError](EditionNotFound(isbn))
        .transact(transactor)

    override def create(edition: Edition)(userId: Id[User]): F[WithAuthorizationByActiveStatus[ServiceErrorOr[Edition]]] =
      authorizationByActiveStatus.authorize(userId) {
        repository
          .create(edition)
          .ifTrue(edition)
          .orElse[ServiceError](FailedToCreateEdition(edition.isbn))
          .transact(transactor)
      }

    override def update(isbn: ISBN, details: EditionDetails)(userId: Id[User]): F[WithAuthorizationByActiveStatus[ServiceErrorOr[Edition]]] =
      authorizationByActiveStatus.authorize(userId) {
        repository
          .update(isbn, details)
          .ifTrue(Edition(isbn, details.title, details.authorIds, details.publisherId, details.publicationDate))
          .orElse[ServiceError](FailedToUpdateEdition(isbn))
          .transact(transactor)
      }

    // TODO Check that there are no open Copy offers for this edition
    override def delete(isbn: ISBN)(userId: Id[User]): F[WithAuthorizationByAdminStatus[ServiceErrorOr[Unit]]] =
      authorizationByAdminStatus.authorize(userId) {
        repository
          .delete(isbn)
          .ifTrue(())
          .orElse[ServiceError](FailedToDeleteEdition(isbn))
          .transact(transactor)
      }
  }

}

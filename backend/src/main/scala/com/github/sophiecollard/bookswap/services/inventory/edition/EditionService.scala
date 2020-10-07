package com.github.sophiecollard.bookswap.services.inventory.edition

import cats.{Monad, ~>}
import com.github.sophiecollard.bookswap.authorization.AuthorizationService
import com.github.sophiecollard.bookswap.authorization.instances._
import com.github.sophiecollard.bookswap.domain.inventory.{Edition, EditionDetails, ISBN}
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.repositories.inventory.EditionRepository
import com.github.sophiecollard.bookswap.services.error.ServiceError._
import com.github.sophiecollard.bookswap.services.error.{ServiceError, ServiceErrorOr}
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

  def create[F[_], G[_]: Monad](
    authorizationByActiveStatus: AuthorizationService[F, Id[User], ByActiveStatus],
    authorizationByAdminStatus: AuthorizationService[F, Id[User], ByAdminStatus],
    repository: EditionRepository[G],
    transactor: G ~> F
  ): EditionService[F] = new EditionService[F] {
    override def get(isbn: ISBN): F[ServiceErrorOr[Edition]] =
      getWithoutTransaction(isbn)
        .transact(transactor)

    override def create(edition: Edition)(userId: Id[User]): F[WithAuthorizationByActiveStatus[ServiceErrorOr[Edition]]] =
      authorizationByActiveStatus.authorize(userId) {
        val result = for {
          _ <- repository
            .get(edition.isbn)
            .emptyOrElse[ServiceError](EditionAlreadyExists(edition.isbn))
            .asEitherT
          _ <- repository
            .create(edition)
            .ifTrue(())
            .orElse[ServiceError](FailedToCreateEdition(edition.isbn))
            .asEitherT
        } yield edition

        result.value.transact(transactor)
      }

    override def update(isbn: ISBN, details: EditionDetails)(userId: Id[User]): F[WithAuthorizationByActiveStatus[ServiceErrorOr[Edition]]] = {
      authorizationByActiveStatus.authorize(userId) {
        val result = for {
          _ <- getWithoutTransaction(isbn).asEitherT
          _ <- repository
            .update(isbn, details)
            .ifTrue(())
            .orElse[ServiceError](FailedToUpdateEdition(isbn))
            .asEitherT
        } yield Edition(isbn, details.title, details.authorIds, details.publisherId, details.publicationDate)

        result.value.transact(transactor)
      }
    }

    // TODO Check that there are no open Copy offers for this edition
    override def delete(isbn: ISBN)(userId: Id[User]): F[WithAuthorizationByAdminStatus[ServiceErrorOr[Unit]]] =
      authorizationByAdminStatus.authorize(userId) {
        val result = for {
          _ <- getWithoutTransaction(isbn).asEitherT
          _ <- repository
            .delete(isbn)
            .ifTrue(())
            .orElse[ServiceError](FailedToDeleteEdition(isbn))
            .asEitherT
        } yield ()

        result.value.transact(transactor)
      }

    private def getWithoutTransaction(isbn: ISBN): G[ServiceErrorOr[Edition]] =
      repository
        .get(isbn)
        .orElse[ServiceError](EditionNotFound(isbn))
  }

}

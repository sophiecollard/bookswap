package com.github.sophiecollard.bookswap.services.inventory.edition

import java.time.ZoneId

import cats.{Monad, ~>}
import com.github.sophiecollard.bookswap.authorization.AuthorizationService
import com.github.sophiecollard.bookswap.authorization.instances._
import com.github.sophiecollard.bookswap.domain.inventory.{CopyPagination, Edition, EditionDetailsUpdate, ISBN}
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.repositories.inventory.{CopiesRepository, EditionsRepository}
import com.github.sophiecollard.bookswap.services.error.ServiceError._
import com.github.sophiecollard.bookswap.services.error.{ServiceError, ServiceErrorOr}
import com.github.sophiecollard.bookswap.syntax._

trait EditionsService[F[_]] {

  /** Fetches an Edition */
  def get(isbn: ISBN): F[ServiceErrorOr[Edition]]

  /** Invoked by a registered user to create an Edition */
  def create(edition: Edition)(userId: Id[User]): F[WithAuthorizationByActiveStatus[ServiceErrorOr[Edition]]]

  /** Invoked by a registered user to update an Edition */
  def update(isbn: ISBN, detailsUpdate: EditionDetailsUpdate)(userId: Id[User]): F[WithAuthorizationByActiveStatus[ServiceErrorOr[Edition]]]

  /** Invoked by an admin user to delete an Edition */
  def delete(isbn: ISBN)(userId: Id[User]): F[WithAuthorizationByAdminStatus[ServiceErrorOr[Unit]]]

}

object EditionsService {

  def create[F[_], G[_]: Monad](
    authorizationByActiveStatus: AuthorizationService[F, Id[User], ByActiveStatus],
    authorizationByAdminStatus: AuthorizationService[F, Id[User], ByAdminStatus],
    editionsRepository: EditionsRepository[G],
    copiesRepository: CopiesRepository[G],
    transactor: G ~> F
  )(
    implicit zoneId: ZoneId // TODO pass from configuration
  ): EditionsService[F] = new EditionsService[F] {
    override def get(isbn: ISBN): F[ServiceErrorOr[Edition]] =
      getWithoutTransaction(isbn)
        .transact(transactor)

    override def create(edition: Edition)(userId: Id[User]): F[WithAuthorizationByActiveStatus[ServiceErrorOr[Edition]]] =
      authorizationByActiveStatus.authorize(userId) {
        val result = for {
          _ <- editionsRepository
            .get(edition.isbn)
            .emptyOrElse[ServiceError](EditionAlreadyExists(edition.isbn))
            .asEitherT
          _ <- editionsRepository
            .create(edition)
            .ifTrue(())
            .orElse[ServiceError](FailedToCreateEdition(edition.isbn))
            .asEitherT
        } yield edition

        result.value.transact(transactor)
      }

    override def update(isbn: ISBN, detailsUpdate: EditionDetailsUpdate)(userId: Id[User]): F[WithAuthorizationByActiveStatus[ServiceErrorOr[Edition]]] = {
      authorizationByActiveStatus.authorize(userId) {
        val result = for {
          edition <- getWithoutTransaction(isbn).asEitherT
          updatedDetails = edition.details.applyUpdate(detailsUpdate)
          updatedEdition <- editionsRepository
            .update(isbn, updatedDetails)
            .ifTrue(Edition(isbn, updatedDetails))
            .orElse[ServiceError](FailedToUpdateEdition(isbn))
            .asEitherT
        } yield updatedEdition

        result.value.transact(transactor)
      }
    }

    override def delete(isbn: ISBN)(userId: Id[User]): F[WithAuthorizationByAdminStatus[ServiceErrorOr[Unit]]] =
      authorizationByAdminStatus.authorize(userId) {
        val result = for {
          _ <- getWithoutTransaction(isbn).asEitherT
          _ <- copiesRepository
            .listForEdition(isbn, CopyPagination.default)
            .ifEmpty(())
            .orElse[ServiceError](EditionStillHasCopiesOnOffer(isbn))
            .asEitherT
          _ <- editionsRepository
            .delete(isbn)
            .ifTrue(())
            .orElse[ServiceError](FailedToDeleteEdition(isbn))
            .asEitherT
        } yield ()

        result.value.transact(transactor)
      }

    private def getWithoutTransaction(isbn: ISBN): G[ServiceErrorOr[Edition]] =
      editionsRepository
        .get(isbn)
        .orElse[ServiceError](EditionNotFound(isbn))
  }

}

package com.github.sophiecollard.bookswap.services

import com.github.sophiecollard.bookswap.domain.inventory.ISBN
import com.github.sophiecollard.bookswap.domain.shared.{Id, Name}
import com.github.sophiecollard.bookswap.domain.user.User

object error {

  sealed abstract class ServiceError(val message: String)

  type ServiceErrorOr[A] = Either[ServiceError, A]

  object ServiceError {

    final case class InvalidState(override val message: String)
      extends ServiceError(message)

    final case class ResourceNotFound[A](resourceName: String, id: Id[A])
      extends ServiceError(
        message = s"$resourceName [${id.value}] was not found."
      )

    final case class EditionNotFound(isbn: ISBN)
      extends ServiceError(
        message = s"Edition [${isbn.value}] was not found."
      )

    final case class UserNameAlreadyTaken(name: Name[User])
      extends ServiceError(
        message = s"User name [$name] is already taken."
      )

    final case class EditionAlreadyExists(isbn: ISBN)
      extends ServiceError(
        message = s"Edition [${isbn.value}] already exists."
      )

    final case class EditionStillHasCopiesOnOffer(isbn: ISBN)
      extends ServiceError(
        message = s"Edition [${isbn.value}] still has copies with status 'available' or 'reserved'."
      )

    final case class FailedToCreateResource[A](resourceName: String, id: Id[A])
      extends ServiceError(
        message = s"$resourceName [${id.value}] could not be created."
      )

    final case class FailedToCreateEdition(isbn: ISBN)
      extends ServiceError(
        message = s"Edition [${isbn.value}] could not be created."
      )

    final case class FailedToUpdateResource[A](resourceName: String, id: Id[A])
      extends ServiceError(
        message = s"$resourceName [${id.value}] could not be updated."
      )

    final case class FailedToUpdateEdition(isbn: ISBN)
      extends ServiceError(
        message = s"Edition [${isbn.value}] could not be updated."
      )

    final case class FailedToDeleteResource[A](resourceName: String, id: Id[A])
      extends ServiceError(
        message = s"$resourceName [${id.value}] could not be deleted."
      )

    final case class FailedToDeleteEdition(isbn: ISBN)
      extends ServiceError(
        message = s"Edition [${isbn.value}] could not be deleted."
      )

  }

}

package com.github.sophiecollard.bookswap

import com.github.sophiecollard.bookswap.domain.inventory.{Copy, ISBN}
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.CopyRequest
import com.github.sophiecollard.bookswap.domain.user.User

object error {

  sealed abstract class AuthorizationError(val message: String)

  type AuthorizationErrorOr[A] = Either[AuthorizationError, A]

  object AuthorizationError {

    final case class NotTheSameUser(firstUserId: Id[User], secondUserId: Id[User])
      extends AuthorizationError(
        message = s"User [${firstUserId.value}] is not the same as User [${secondUserId.value}]."
      )

    final case class NotAnActiveUser(userId: Id[User])
      extends AuthorizationError(
        message = s"User [${userId.value}] is not an active user."
      )

    final case class NotAnAdmin(userId: Id[User])
      extends AuthorizationError(
        message = s"User [${userId.value}] is not an admin."
      )

    final case class NotTheCopyOwner(userId: Id[User], copyId: Id[Copy])
      extends AuthorizationError(
        message = s"User [${userId.value}] is not the owner of Copy [${copyId.value}]."
      )

    final case class NotTheRequestedCopyOwner(userId: Id[User], copyRequestId: Id[CopyRequest])
      extends AuthorizationError(
        message = s"User [${userId.value}] is not the owner of the copy requested in CopyRequest [${copyRequestId.value}]."
      )

    final case class NotTheRequestIssuer(userId: Id[User], copyRequestId: Id[CopyRequest])
      extends AuthorizationError(
        message = s"User [${userId.value}] did not issue CopyRequest [${copyRequestId.value}]."
      )

  }

  sealed abstract class ServiceError(val message: String)

  type ServiceErrorOr[A] = Either[ServiceError, A]

  object ServiceError {

    final case class InvalidState(override val message: String)
      extends ServiceError(message)

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

    final case class ResourceNotFound[A](resourceName: String, id: Id[A])
      extends ServiceError(
        message = s"$resourceName [${id.value}] was not found."
      )

    final case class EditionNotFound(isbn: ISBN)
      extends ServiceError(
        message = s"Edition [${isbn.value}] was not found."
      )

  }

}

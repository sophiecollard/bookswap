package com.github.sophiecollard.bookswap.error

import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.CopyRequest
import com.github.sophiecollard.bookswap.domain.user.User

sealed trait Error {
  def message: String
}

object Error {

  sealed abstract class AuthorizationError(override val message: String) extends Error

  final case class NotAnAdmin(userId: Id[User])
    extends AuthorizationError(
      message = s"User [${userId.value}] is not an admin."
    )

  final case class NotTheCopyOwner(userId: Id[User], copyRequestId: Id[CopyRequest])
    extends AuthorizationError(
      message = s"User [${userId.value}] is not the owner of the copy requested in CopyRequest [${copyRequestId.value}]."
    )

  final case class NotTheRequestIssuer(userId: Id[User], copyRequestId: Id[CopyRequest])
    extends AuthorizationError(
      message = s"User [${userId.value}] did not issue CopyRequest [${copyRequestId.value}]."
    )

  type AuthorizationErrorOr[A] = Either[AuthorizationError, A]

  sealed abstract class TransactionError(override val message: String) extends Error

  final case class InvalidState(override val message: String)
    extends TransactionError(message)

  final case class ResourceNotFound[A](resourceName: String, id: Id[A])
    extends TransactionError(
      message = s"$resourceName [${id.value}] was not found."
    )

  type TransactionErrorOr[A] = Either[TransactionError, A]

}

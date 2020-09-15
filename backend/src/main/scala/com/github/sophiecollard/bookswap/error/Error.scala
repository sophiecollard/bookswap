package com.github.sophiecollard.bookswap.error

import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.CopyRequest
import com.github.sophiecollard.bookswap.domain.user.User

sealed trait Error {
  def message: String
}

object Error {

  sealed abstract class AuthorizationError(override val message: String) extends Error

  final case class NotAnAdminUser(userId: Id[User]) extends AuthorizationError(
    message = s"User [${userId.value}] is not an admin."
  )

  final case class NoPermissionOnCopyRequest(userId: Id[User], copyRequestId: Id[CopyRequest])
    extends AuthorizationError(
      message = s"User [${userId.value}] does not have permission to modify CopyRequest [${copyRequestId.value}]."
    )

  type AuthorizationErrorOr[A] = Either[AuthorizationError, A]

  sealed abstract class TransactionError(override val message: String) extends Error

  type TransactionErrorOr[A] = Either[TransactionError, A]

}

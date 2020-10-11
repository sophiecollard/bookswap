package com.github.sophiecollard.bookswap.authorization

import com.github.sophiecollard.bookswap.domain.inventory.Copy
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.CopyRequest
import com.github.sophiecollard.bookswap.domain.user.User

object error {

  sealed abstract class AuthorizationError(val message: String)

  type AuthorizationErrorOr[A] = Either[AuthorizationError, A]

  object AuthorizationError {

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

}

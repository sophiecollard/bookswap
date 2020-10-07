package com.github.sophiecollard.bookswap.services.user

import cats.Functor
import cats.implicits._
import com.github.sophiecollard.bookswap.authorization.error.AuthorizationError
import com.github.sophiecollard.bookswap.authorization.error.AuthorizationError.NotAThreadParticipant
import com.github.sophiecollard.bookswap.authorization.{AuthorizationService, WithAuthorization}
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.repositories.user.ThreadRepository
import com.github.sophiecollard.bookswap.syntax._

object authorization {

  trait ByThreadParticipant

  type WithAuthorizationByThreadParticipant[R] = WithAuthorization[R, ByThreadParticipant]

  final case class AuthorizationInput(userId: Id[User], threadId: Id[Thread])

  def byThreadParticipant[F[_]: Functor](
    threadRepository: ThreadRepository[F]
  ): AuthorizationService[F, AuthorizationInput, ByThreadParticipant] =
    AuthorizationService.create { case AuthorizationInput(userId, threadId) =>
      threadRepository
        .getParticipants(threadId)
        .map(_.exists(_ == userId))
        .ifTrue(Right())
        .orElse[AuthorizationError](NotAThreadParticipant(userId, threadId))
    }

}

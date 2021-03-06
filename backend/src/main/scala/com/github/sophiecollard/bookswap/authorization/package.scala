package com.github.sophiecollard.bookswap

import cats.implicits._
import cats.Monad
import com.github.sophiecollard.bookswap.authorization.error.{AuthorizationError, AuthorizationErrorOr}

package object authorization {

  /**
    * Type used to wrap the result R of an operation requiring authorization.
    *
    * The type parameter Tag serves as a sort of tag to distinguish between different types of authorization.
    * When using multiple authorization services (for instance: one for authorizing admin users and another
    * for authorizing resource owners) this tag helps the compiler enforce that the right kind of authorization
    * is used.
    */
  sealed abstract class WithAuthorization[+R, Tag] {
    final def isSuccess: Boolean = this match {
      case Success(_) => true
      case Failure(_) => false
    }

    final def isFailure: Boolean =
      !isSuccess

    final def toEither: Either[AuthorizationError, R] = this match {
      case Success(result) => Right(result)
      case Failure(error) => Left(error)
    }

    final def unsafeResult: R = this match {
      case Success(result) => result
      case Failure(_) => throw new RuntimeException("Authorization was a Failure")
    }

    final def unsafeError: AuthorizationError = this match {
      case Success(_) => throw new RuntimeException("Authorization was a Success")
      case Failure(error) => error
    }
  }

  object WithAuthorization {
    def failure[R, Tag](error: AuthorizationError): WithAuthorization[R, Tag] =
      Failure(error)
  }

  sealed abstract case class Success[R, Tag](result: R) extends WithAuthorization[R, Tag]

  final case class Failure[Tag](error: AuthorizationError) extends WithAuthorization[Nothing, Tag]

  trait AuthorizationService[F[_], Input, Tag] {
    def authorize[R](input: Input)(ifAuthorized: => F[R]): F[WithAuthorization[R, Tag]]
  }

  object AuthorizationService {
    def create[F[_], Input, Tag](
      checkAuthorization: Input => F[AuthorizationErrorOr[Unit]]
    )(
      implicit F: Monad[F]
    ): AuthorizationService[F, Input, Tag] =
      new AuthorizationService[F, Input, Tag] {
        override def authorize[R](input: Input)(ifAuthorized: => F[R]): F[WithAuthorization[R, Tag]] =
          checkAuthorization(input).flatMap {
            case Right(_) => ifAuthorized.map(new Success[R, Tag](_) {})
            case Left(e) => WithAuthorization.failure[R, Tag](e).pure[F]
          }
      }
  }

}

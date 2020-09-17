package com.github.sophiecollard.bookswap.service

import cats.Monad
import cats.implicits._
import com.github.sophiecollard.bookswap.error.Error.{AuthorizationError, AuthorizationErrorOr}

package object authorization {

  /**
    * Type used to wrap the result R of an operation requiring authorization.
    *
    * The type parameter Tag serves as a sort of tag to distinguish between different types of authorization.
    * When using multiple authorization services (for instance: one for authorizing admin users and another
    * for authorizing resource owners) this tag helps the compiler enforce that the right kind of authorization
    * is used.
    */
  sealed abstract class WithAuthorization[+R, Tag]

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
            case Left(e)  => WithAuthorization.failure[R, Tag](e).pure[F]
          }
      }
  }

}

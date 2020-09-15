package com.github.sophiecollard.bookswap.service

import cats.Monad
import cats.implicits._
import com.github.sophiecollard.bookswap.error.Error.{AuthorizationError, AuthorizationErrorOr}

package object authorization {

  sealed abstract class WithAuthorization[+R]

  object WithAuthorization {
    def failure[R](error: AuthorizationError): WithAuthorization[R] =
      Failure(error)
  }

  sealed abstract case class Success[R](result: R) extends WithAuthorization[R]

  final case class Failure(error: AuthorizationError) extends WithAuthorization[Nothing]

  trait AuthorizationService[F[_], Input] {
    def authorize[R](input: Input)(ifAuthorized: => F[R]): F[WithAuthorization[R]]
  }

  object AuthorizationService {
    def create[F[_], Input](
      checkAuthorization: Input => F[AuthorizationErrorOr[Unit]]
    )(
      implicit F: Monad[F]
    ): AuthorizationService[F, Input] =
      new AuthorizationService[F, Input] {
        override def authorize[R](input: Input)(ifAuthorized: => F[R]): F[WithAuthorization[R]] =
          checkAuthorization(input).flatMap {
            case Right(_) => ifAuthorized.map(new Success[R](_) {})
            case Left(e)  => WithAuthorization.failure[R](e).pure[F]
          }
      }
  }

}

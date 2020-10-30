package com.github.sophiecollard.bookswap

import cats.effect.IO
import com.github.sophiecollard.bookswap.authorization.WithAuthorization
import com.github.sophiecollard.bookswap.authorization.error.AuthorizationError
import com.github.sophiecollard.bookswap.services.error.{ServiceError, ServiceErrorOr}
import org.http4s.{EntityDecoder, Response}
import org.scalatest.Assertion

object specsyntax {

  implicit class ResponseSyntax(val response: Response[IO]) extends AnyVal {
    def withBodyAs[B](ifBody: B => Assertion)(implicit ev: EntityDecoder[IO, B]): Assertion = {
      val decodeResult = response.attemptAs[B].value.unsafeRunSync()
      assert(decodeResult.isRight)
      ifBody(decodeResult.toOption.get)
    }
  }

  def withFailedAuthorization[R, Tag](
    authorizationResult: WithAuthorization[R, Tag]
  )(
    ifFailure: AuthorizationError => Assertion
  ): Assertion = {
    assert(authorizationResult.isFailure)
    ifFailure(authorizationResult.unsafeError)
  }

  def withSuccessfulAuthorization[R, Tag](
    authorizationResult: WithAuthorization[R, Tag]
  )(
    ifSuccessful: R => Assertion
  ): Assertion = {
    assert(authorizationResult.isSuccess)
    ifSuccessful(authorizationResult.unsafeResult)
  }

  def withServiceError[R](
    ifError: ServiceError => Assertion
  )(
    serviceErrorOr: ServiceErrorOr[R]
  ): Assertion = {
    assert(serviceErrorOr.isLeft)
    ifError(serviceErrorOr.swap.toOption.get)
  }

  def withNoServiceError[R](
    ifNoError: R => Assertion
  )(
    serviceErrorOr: ServiceErrorOr[R]
  ): Assertion = {
    assert(serviceErrorOr.isRight)
    ifNoError(serviceErrorOr.toOption.get)
  }

  def withSome[A](maybeA: Option[A])(ifSome: A => Assertion): Assertion = {
    assert(maybeA.isDefined)
    ifSome(maybeA.get)
  }

  def withNone[A](maybeA: Option[A])(ifNone: => Assertion): Assertion = {
    assert(maybeA.isEmpty)
    ifNone
  }

  def withLeft[E, A](
    maybeE: Either[E, A]
  )(
    ifLeft: E => Assertion
  ): Assertion = {
    assert(maybeE.isLeft)
    ifLeft(maybeE.swap.toOption.get)
  }

  def withRight[E, A](
    maybeA: Either[E, A]
  )(
    ifRight: A => Assertion
  ): Assertion = {
    assert(maybeA.isRight)
    ifRight(maybeA.toOption.get)
  }

}

package com.github.sophiecollard.bookswap.services

import com.github.sophiecollard.bookswap.error.Error.{AuthorizationError, ServiceErrorOr}
import com.github.sophiecollard.bookswap.services.authorization.WithAuthorization
import org.scalatest.Assertion

package object testsyntax {

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

  def withNoServiceError[R](
    ifNoError: R => Assertion
  )(
    serviceErrorOr: ServiceErrorOr[R]
  ): Assertion = {
    assert(serviceErrorOr.isRight)
    ifNoError(serviceErrorOr.toOption.get)
  }

}

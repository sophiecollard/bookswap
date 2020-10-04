package com.github.sophiecollard.bookswap.api

import cats.Applicative
import cats.implicits._
import com.github.sophiecollard.bookswap.authorization
import com.github.sophiecollard.bookswap.error.ServiceErrorOr
import org.http4s.Response
import org.http4s.dsl.io.{NotFound, Unauthorized}

object syntax {

  implicit class ConverterSyntax[A](val value: A) extends AnyVal {
    def convertTo[B](implicit ev: Converter[A, B]): B =
      ev.convertTo(value)
  }

  def withSuccessfulAuthorization[F[_]: Applicative, R, Tag](
    ifSuccessful: R => F[Response[F]]
  )(
    authorizationResult: authorization.WithAuthorization[R, Tag]
  ): F[Response[F]] =
    authorizationResult match {
      case authorization.Success(result) =>
        ifSuccessful(result)
      case authorization.Failure(_) =>
        // TODO Include details
        Response[F](Unauthorized).pure[F]
    }

  def withNoServiceError[F[_]: Applicative, R](
    ifNoError: R => F[Response[F]]
  )(
    serviceErrorOr: ServiceErrorOr[R]
  ): F[Response[F]] =
    serviceErrorOr match {
      case Right(result) =>
        ifNoError(result)
      case Left(_) =>
        // TODO Handle different types of service errors
        Response[F](NotFound).pure[F]
    }

}

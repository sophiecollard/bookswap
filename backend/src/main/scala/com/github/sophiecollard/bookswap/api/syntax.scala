package com.github.sophiecollard.bookswap.api

import cats.{Applicative, Monad}
import cats.implicits._
import com.github.sophiecollard.bookswap.api.error.ApiError
import com.github.sophiecollard.bookswap.authorization
import com.github.sophiecollard.bookswap.services.error.ServiceErrorOr
import org.http4s._
import org.http4s.dsl.io.Unauthorized

object syntax {

  implicit class ConverterSyntax[A](val value: A) extends AnyVal {
    def convertTo[B](implicit ev: Converter[A, B]): B =
      ev.convertTo(value)
  }

  implicit class RequestSyntax[F[_]](val request: Request[F]) extends AnyVal {
    def withBodyAs[B](
      ifNoDecodeFailure: B => F[Response[F]]
    )(
      implicit F: Monad[F],
      decoder: EntityDecoder[F, B]
    ): F[Response[F]] =
      request
        .attemptAs[B]
        .leftMap(_.toHttpResponse[F](request.httpVersion))
        .semiflatMap(ifNoDecodeFailure)
        .merge
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
      case Left(serviceError) =>
        ApiError.fromServiceError(serviceError).response
    }

}

package com.github.sophiecollard.bookswap.api

import cats.Applicative
import com.github.sophiecollard.bookswap.services.error.ServiceError
import com.github.sophiecollard.bookswap.services.error.ServiceError.{EditionNotFound, ResourceNotFound}
import io.circe.syntax._
import io.circe.{Encoder, Json}
import org.http4s.Response
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl

object error {

  def responseFromServiceError[F[_]: Applicative](error: ServiceError): F[Response[F]] = {
    object dsl extends Http4sDsl[F]
    import dsl._

    error match {
      case resourceNotFound: ResourceNotFound[_] =>
        NotFound(ErrorResponseBody(resourceNotFound.message))
      case editionNotFound: EditionNotFound =>
        NotFound(ErrorResponseBody(editionNotFound.message))
      case otherServiceError =>
        BadRequest(ErrorResponseBody(otherServiceError.message))
    }
  }

  final case class ErrorResponseBody(message: String)

  object ErrorResponseBody {

    implicit val encoder: Encoder[ErrorResponseBody] =
      Encoder.instance { error =>
        Json.obj(
          "error" := error.message.asJson
        )
      }

  }

}

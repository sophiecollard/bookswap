package com.github.sophiecollard.bookswap.api

import cats.Applicative
import cats.implicits._
import com.github.sophiecollard.bookswap.services.error.ServiceError
import io.circe.syntax._
import io.circe.{Encoder, Json}
import org.http4s.Response
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl

object error {

  sealed abstract class ApiError(val responseBody: ErrorResponseBody) {
    final def response[F[_]: Applicative]: F[Response[F]] = {
      object dsl extends Http4sDsl[F]
      import dsl._

      this match {
        case error @ ApiError.ResourceNotFound(_) =>
          NotFound(error.responseBody)
        case error @ ApiError.ResourceAlreadyExists(_) =>
          Conflict(error.responseBody)
        case error @ ApiError.FailedToCreateResource(_) =>
          InternalServerError(error.responseBody)
        case error @ ApiError.FailedToUpdateResource(_) =>
          InternalServerError(error.responseBody)
        case error @ ApiError.FailedToDeleteResource(_) =>
          InternalServerError(error.responseBody)
        case error @ ApiError.UnexpectedError =>
          InternalServerError(error.responseBody)
      }
    }
  }

  object ApiError {

    final case class ResourceNotFound(message: String) extends ApiError(
      responseBody = ErrorResponseBody(
        message = message,
        help = "Please check that the resource identifier is correct.".some
      )
    )

    final case class ResourceAlreadyExists(message: String) extends ApiError(
      responseBody = ErrorResponseBody(
        message = message,
        help = "Please try updating the existing resource instead.".some
      )
    )

    final case class FailedToCreateResource(resourceName: String) extends ApiError(
      responseBody = ErrorResponseBody(
        message = s"Unexpected failure while creating $resourceName.",
        help = "Please try one more time. If the error persists, this is probably a bug.".some
      )
    )

    final case class FailedToUpdateResource(resourceName: String) extends ApiError(
      responseBody = ErrorResponseBody(
        message = s"Unexpected failure while updating $resourceName.",
        help = "Please try one more time. If the error persists, this is probably a bug.".some
      )
    )

    final case class FailedToDeleteResource(resourceName: String) extends ApiError(
      responseBody = ErrorResponseBody(
        message = s"Unexpected failure while deleting $resourceName.",
        help = "Please try one more time. If the error persists, this is probably a bug.".some
      )
    )

    case object UnexpectedError extends ApiError(
      responseBody = ErrorResponseBody(
        message = "An unexpected server error occurred."
      )
    )

    def fromServiceError(serviceError: ServiceError): ApiError =
      serviceError match {
        case error @ ServiceError.ResourceNotFound(_, _) =>
          ResourceNotFound(error.message)
        case error @ ServiceError.EditionNotFound(_) =>
          ResourceNotFound(error.message)
        case ServiceError.FailedToCreateResource(resourceName, _) =>
          FailedToCreateResource(resourceName)
        case ServiceError.FailedToCreateEdition(isbn) =>
          ResourceAlreadyExists(s"An Edition with ISBN [${isbn.value}] already exists.")
        case ServiceError.FailedToUpdateResource(resourceName, _) =>
          FailedToUpdateResource(resourceName)
        case ServiceError.FailedToUpdateEdition(_) =>
          FailedToUpdateResource("Edition")
        case ServiceError.FailedToDeleteResource(resourceName, _) =>
          FailedToDeleteResource(resourceName)
        case ServiceError.FailedToDeleteEdition(_) =>
          FailedToDeleteResource("Edition")
        case _ =>
          UnexpectedError
      }

  }

  final case class ErrorResponseBody(message: String, help: Option[String] = None)

  object ErrorResponseBody {
    implicit val encoder: Encoder[ErrorResponseBody] =
      Encoder.instance { error =>
        Json.obj(
          "error" := error.message.asJson,
          "help" := error.help.asJson
        )
      }
  }

}

package com.github.sophiecollard.bookswap.api.inventory.copy

import java.util.UUID

import cats.effect.Sync
import cats.implicits._
import com.github.sophiecollard.bookswap.api.syntax._
import com.github.sophiecollard.bookswap.domain.inventory.Copy
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.services.inventory.copy.CopyService
import com.github.sophiecollard.bookswap.{authorization, domain}
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, Response}

import scala.util.Try

object endpoints {

  def create[F[_]](
    service: CopyService[F]
  )(
    implicit F: Sync[F]
  ): HttpRoutes[F] = {
    object dsl extends Http4sDsl[F]
    import dsl._

    // TODO Obtain Id[User] from AuthMiddleware
    val userId = Id.generate[User]

    HttpRoutes.of[F] {
      case req @ POST -> Root =>
        req.as[CreateCopyRequestBody].flatMap { requestBody =>
          val (isbn, condition) = requestBody.convertTo[(domain.inventory.ISBN, domain.inventory.Condition)]
          service.create(isbn, condition)(userId).flatMap {
            case authorization.Success(result) =>
              result match {
                case Right(copy) =>
                  Ok(copy.convertTo[CopyResponseBody])
                case Left(_) =>
                  Response[F](NotFound).pure[F] // TODO include details
              }
            case authorization.Failure(_) =>
              Response[F](Unauthorized).pure[F] // TODO include details
          }
        }
      case GET -> Root / CopyIdVar(copyId) =>
        service.get(copyId).flatMap {
          case Right(copy) =>
            Ok(copy.convertTo[CopyResponseBody])
          case Left(_) =>
            Response[F](NotFound).pure[F] // TODO include details
        }
      case req @ PUT -> Root / CopyIdVar(copyId) =>
        req.as[UpdateCopyRequestBody].flatMap { requestBody =>
          val condition = requestBody.convertTo[domain.inventory.Condition]
          service.updateCondition(copyId, condition)(userId).flatMap {
            case authorization.Success(result) =>
              result match {
                case Right(copy) =>
                  Ok(copy.convertTo[CopyResponseBody])
                case Left(_) =>
                  Response[F](NotFound).pure[F] // TODO include details
              }
            case authorization.Failure(_) =>
              Response[F](Unauthorized).pure[F] // TODO include details
          }
        }
      case DELETE -> Root / CopyIdVar(copyId) =>
        service.withdraw(copyId)(userId).flatMap {
          case authorization.Success(result) =>
            result match {
              case Right(copy) =>
                Ok(copy.convertTo[CopyResponseBody])
              case Left(_) =>
                Response[F](NotFound).pure[F] // TODO include details
            }
          case authorization.Failure(_) =>
            Response[F](Unauthorized).pure[F] // TODO return details
        }
    }
  }

  object CopyIdVar {
    def unapply(str: String): Option[Id[Copy]] =
      if (!str.isEmpty)
        Try(UUID.fromString(str))
          .map(Id.apply[Copy])
          .toOption
      else
        None
  }

}

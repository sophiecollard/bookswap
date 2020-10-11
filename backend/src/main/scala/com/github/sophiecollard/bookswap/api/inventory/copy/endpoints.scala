package com.github.sophiecollard.bookswap.api.inventory.copy

import java.time.{LocalDateTime, ZoneId}

import cats.effect.Sync
import cats.implicits._
import com.github.sophiecollard.bookswap.api.instances._
import com.github.sophiecollard.bookswap.api.syntax._
import com.github.sophiecollard.bookswap.domain.inventory.{Condition, CopyPagination, ISBN}
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.services.inventory.copy.CopyService
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.{OptionalQueryParamDecoderMatcher, QueryParamDecoderMatcher}
import org.http4s.server.AuthMiddleware

object endpoints {

  def create[F[_]](
    authMiddleware: AuthMiddleware[F, Id[User]],
    service: CopyService[F]
  )(
    implicit F: Sync[F]
  ): HttpRoutes[F] = {
    object dsl extends Http4sDsl[F]
    import dsl._

    // TODO Pass in configuration
    implicit val zoneId = ZoneId.of("UTC")

    val privateRoutes: AuthedRoutes[Id[User], F] = AuthedRoutes.of[Id[User], F] {
      case authedReq @ POST -> Root as userId =>
        authedReq.req.as[CreateCopyRequestBody].flatMap { requestBody =>
          val (isbn, condition) = requestBody.convertTo[(ISBN, Condition)]
          service.create(isbn, condition)(userId).flatMap {
            withSuccessfulAuthorization {
              withNoServiceError { copy =>
                Created(copy.convertTo[CopyResponseBody])
              }
            }
          }
        }
      case authedReq @ PUT -> Root / CopyIdVar(copyId) as userId =>
        authedReq.req.as[UpdateCopyRequestBody].flatMap { requestBody =>
          val condition = requestBody.convertTo[Condition]
          service.updateCondition(copyId, condition)(userId).flatMap {
            withSuccessfulAuthorization {
              withNoServiceError { copy =>
                Ok(copy.convertTo[CopyResponseBody])
              }
            }
          }
        }
      case DELETE -> Root / CopyIdVar(copyId) as userId =>
        service.withdraw(copyId)(userId).flatMap {
          withSuccessfulAuthorization {
            withNoServiceError { copy =>
              Ok(copy.convertTo[CopyResponseBody])
            }
          }
        }
    }

    val publicRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
      case GET -> Root :? ISBNQueryParamMatcher(isbn) +&
        OfferedOnOrBeforeParamMatcher(maybePageOffset) +&
        PageSizeParamMatcher(maybePageSize) =>
        val pagination = CopyPagination.fromOptionalValues(maybePageOffset, maybePageSize)
        service.listForEdition(isbn, pagination).flatMap { copies =>
          Ok(copies.map(_.convertTo[CopyResponseBody]))
        }
      case GET -> Root :? OfferedByQueryParamMatcher(offeredBy) +&
        OfferedOnOrBeforeParamMatcher(maybePageOffset) +&
        PageSizeParamMatcher(maybePageSize) =>
        val pagination = CopyPagination.fromOptionalValues(maybePageOffset, maybePageSize)
        service.listForOwner(offeredBy, pagination).flatMap { copies =>
          Ok(copies.map(_.convertTo[CopyResponseBody]))
        }
      case GET -> Root / CopyIdVar(copyId) =>
        service.get(copyId).flatMap {
          withNoServiceError { copy =>
            Ok(copy.convertTo[CopyResponseBody])
          }
        }
    }

    authMiddleware(privateRoutes) <+> publicRoutes
  }

  object OfferedOnOrBeforeParamMatcher extends OptionalQueryParamDecoderMatcher[LocalDateTime]("page_offset")
  object OfferedByQueryParamMatcher extends QueryParamDecoderMatcher[Id[User]]("offered_by")

}

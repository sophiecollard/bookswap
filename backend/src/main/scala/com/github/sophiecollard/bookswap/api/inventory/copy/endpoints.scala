package com.github.sophiecollard.bookswap.api.inventory.copy

import java.time.{LocalDateTime, ZoneId}

import cats.effect.Sync
import cats.implicits._
import com.github.sophiecollard.bookswap.api.instances._
import com.github.sophiecollard.bookswap.api.syntax._
import com.github.sophiecollard.bookswap.domain
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.services.inventory.copy.CopyService
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.dsl.impl.{OptionalQueryParamDecoderMatcher, QueryParamDecoderMatcher}

object endpoints {

  def create[F[_]](
    service: CopyService[F]
  )(
    implicit F: Sync[F]
  ): HttpRoutes[F] = {
    object dsl extends Http4sDsl[F]
    import dsl._

    // TODO Obtain Id[User] from AuthMiddleware
    val userId = domain.shared.Id.generate[User]
    // TODO Pass in configuration
    implicit val zoneId = ZoneId.of("UTC")

    HttpRoutes.of[F] {
      case req @ POST -> Root =>
        req.as[CreateCopyRequestBody].flatMap { requestBody =>
          val (isbn, condition) = requestBody.convertTo[(domain.inventory.ISBN, domain.inventory.Condition)]
          service.create(isbn, condition)(userId).flatMap {
            withSuccessfulAuthorization {
              withNoServiceError { copy =>
                Ok(copy.convertTo[CopyResponseBody])
              }
            }
          }
        }
      case GET -> Root :? ISBNQueryParamMatcher(isbn) +&
        OfferedOnOrBeforeParamMatcher(maybePageOffset) +&
        PageSizeParamMatcher(maybePageSize) =>
        val pagination = CopyPagination(maybePageOffset, maybePageSize).convertTo[domain.inventory.CopyPagination]
        service.listForEdition(isbn, pagination).flatMap { copies =>
          Ok(copies.map(_.convertTo[CopyResponseBody]))
        }
      case GET -> Root :? OfferedByQueryParamMatcher(offeredBy) +&
        OfferedOnOrBeforeParamMatcher(maybePageOffset) +&
        PageSizeParamMatcher(maybePageSize) =>
        val pagination = CopyPagination(maybePageOffset, maybePageSize).convertTo[domain.inventory.CopyPagination]
        service.listForOwner(offeredBy, pagination).flatMap { copies =>
          Ok(copies.map(_.convertTo[CopyResponseBody]))
        }
      case GET -> Root / CopyIdVar(copyId) =>
        service.get(copyId).flatMap {
          withNoServiceError { copy =>
            Ok(copy.convertTo[CopyResponseBody])
          }
        }
      case req @ PUT -> Root / CopyIdVar(copyId) =>
        req.as[UpdateCopyRequestBody].flatMap { requestBody =>
          val condition = requestBody.convertTo[domain.inventory.Condition]
          service.updateCondition(copyId, condition)(userId).flatMap {
            withSuccessfulAuthorization {
              withNoServiceError { copy =>
                Ok(copy.convertTo[CopyResponseBody])
              }
            }
          }
        }
      case DELETE -> Root / CopyIdVar(copyId) =>
        service.withdraw(copyId)(userId).flatMap {
          withSuccessfulAuthorization {
            withNoServiceError { copy =>
              Ok(copy.convertTo[CopyResponseBody])
            }
          }
        }
    }
  }

  object OfferedOnOrBeforeParamMatcher extends OptionalQueryParamDecoderMatcher[LocalDateTime]("page_offset")
  object OfferedByQueryParamMatcher extends QueryParamDecoderMatcher[Id[User]]("offered_by")

}

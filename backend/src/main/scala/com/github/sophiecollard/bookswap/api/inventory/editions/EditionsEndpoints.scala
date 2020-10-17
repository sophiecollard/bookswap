package com.github.sophiecollard.bookswap.api.inventory.editions

import cats.effect.Sync
import cats.implicits._
import com.github.sophiecollard.bookswap.api.instances.http4s.ISBNVar
import com.github.sophiecollard.bookswap.api.syntax._
import com.github.sophiecollard.bookswap.domain.inventory.{Edition, EditionDetailsUpdate}
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.services.inventory.edition.EditionService
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware

object EditionsEndpoints {

  def create[F[_]](
    authMiddleware: AuthMiddleware[F, Id[User]],
    service: EditionService[F]
  )(
    implicit F: Sync[F]
  ): HttpRoutes[F] = {
    object dsl extends Http4sDsl[F]
    import dsl._

    val privateRoutes: AuthedRoutes[Id[User], F] = AuthedRoutes.of[Id[User], F] {
      case authedReq @ POST -> Root as userId =>
        authedReq.req.withBodyAs[CreateEditionRequestBody] { requestBody =>
          val edition = requestBody.convertTo[Edition]
          service.create(edition)(userId).flatMap {
            withSuccessfulAuthorization {
              withNoServiceError { _ =>
                Created(edition.convertTo[EditionResponseBody])
              }
            }
          }
        }
      case authedReq @ PUT -> Root / ISBNVar(isbn) as userId =>
        authedReq.req.withBodyAs[UpdateEditionRequestBody] { requestBody =>
          val editionDetailsUpdate = requestBody.convertTo[EditionDetailsUpdate]
          service.update(isbn, editionDetailsUpdate)(userId).flatMap {
            withSuccessfulAuthorization {
              withNoServiceError { edition =>
                Ok(edition.convertTo[EditionResponseBody])
              }
            }
          }
        }
      case DELETE -> Root / ISBNVar(isbn) as userId =>
        service.delete(isbn)(userId).flatMap {
          withSuccessfulAuthorization {
            withNoServiceError { _ =>
              Ok()
            }
          }
        }
    }

    val publicRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
      case GET -> Root / ISBNVar(isbn) =>
        service.get(isbn).flatMap {
          withNoServiceError { edition =>
            Ok(edition.convertTo[EditionResponseBody])
          }
        }
    }

    authMiddleware(privateRoutes) <+> publicRoutes
  }

}

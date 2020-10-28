package com.github.sophiecollard.bookswap.api.inventory.authors

import cats.effect.Sync
import cats.implicits._
import com.github.sophiecollard.bookswap.api.instances.http4s.AuthorIdVar
import com.github.sophiecollard.bookswap.api.syntax._
import com.github.sophiecollard.bookswap.domain.inventory.Author
import com.github.sophiecollard.bookswap.domain.shared.{Id, Name}
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.services.inventory.authors.AuthorsService
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware

object AuthorsEndpoints {

  def create[F[_]](
    authMiddleware: AuthMiddleware[F, Id[User]],
    service: AuthorsService[F]
  )(
    implicit F: Sync[F]
  ): HttpRoutes[F] = {
    object dsl extends Http4sDsl[F]
    import dsl._

    val privateRoutes: AuthedRoutes[Id[User], F] = AuthedRoutes.of[Id[User], F] {
      case authedRequest @ POST -> Root as userId =>
        authedRequest.req.withBodyAs[CreateAuthorRequestBody] { requestBody =>
          val name = requestBody.convertTo[Name[Author]]
          service.create(name)(userId).flatMap {
            withSuccessfulAuthorization {
              withNoServiceError { author =>
                Created(author.convertTo[AuthorResponseBody])
              }
            }
          }
        }
      case DELETE -> Root / AuthorIdVar(authorId) as userId =>
        service.delete(authorId)(userId).flatMap {
          withSuccessfulAuthorization {
            withNoServiceError { _ =>
              Ok()
            }
          }
        }
    }

    val publicRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
      case GET -> Root / AuthorIdVar(authorId) =>
        service.get(authorId).flatMap {
          withNoServiceError { author =>
            Ok(author.convertTo[AuthorResponseBody])
          }
        }
    }

    authMiddleware(privateRoutes) <+> publicRoutes
  }

}

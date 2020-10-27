package com.github.sophiecollard.bookswap.api.users

import cats.effect.Sync
import cats.implicits._
import com.github.sophiecollard.bookswap.api.instances.http4s.UserIdVar
import com.github.sophiecollard.bookswap.api.syntax._
import com.github.sophiecollard.bookswap.domain.shared.{Id, Name}
import com.github.sophiecollard.bookswap.domain.user.{User, UserStatus}
import com.github.sophiecollard.bookswap.services.user.UsersService
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware

object UsersEndpoints {

  def create[F[_]](
    authMiddleware: AuthMiddleware[F, Id[User]],
    service: UsersService[F]
  )(
    implicit F: Sync[F]
  ): HttpRoutes[F] = {
    object dsl extends Http4sDsl[F]
    import dsl._

    val privateRoutes: AuthedRoutes[Id[User], F] = AuthedRoutes.of[Id[User], F] {
      case authedRequest @ PUT -> Root / UserIdVar(id) as userId =>
        authedRequest.req.withBodyAs[UpdateUserRequestBody] { requestBody =>
          val status = requestBody.convertTo[UserStatus]
          service.updateStatus(id, status)(userId).flatMap {
            withSuccessfulAuthorization {
              withNoServiceError { _ =>
                Ok()
              }
            }
          }
        }
      case DELETE -> Root as userId =>
        service.softDelete(userId).flatMap {
          withNoServiceError { _ =>
            Ok()
          }
        }
      case DELETE -> Root / UserIdVar(id) as userId =>
        service.hardDelete(id)(userId).flatMap {
          withSuccessfulAuthorization {
            withNoServiceError { _ =>
              Ok()
            }
          }
        }
    }

    val publicRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
      case req @ POST -> Root =>
        req.withBodyAs[CreateUserRequestBody] { requestBody =>
          val name = requestBody.convertTo[Name[User]]
          service.create(name).flatMap {
            withNoServiceError { user =>
              Created(user.convertTo[UserResponseBody])
            }
          }
        }
      case GET -> Root / UserIdVar(id) =>
        service.get(id).flatMap {
          withNoServiceError { user =>
            Ok(user.convertTo[UserResponseBody])
          }
        }
    }

    authMiddleware(privateRoutes) <+> publicRoutes
  }

}

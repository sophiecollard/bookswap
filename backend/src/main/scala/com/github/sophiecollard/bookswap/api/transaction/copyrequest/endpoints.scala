package com.github.sophiecollard.bookswap.api.transaction.copyrequest

import cats.effect.Sync
import cats.implicits._
import com.github.sophiecollard.bookswap.api.syntax._
import com.github.sophiecollard.bookswap.domain.inventory.Copy
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.services.transaction.copyrequest.CopyRequestService
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.dsl.Http4sDsl

object endpoints {

  def create[F[_]](
    service: CopyRequestService[F]
  )(
    implicit F: Sync[F]
  ): HttpRoutes[F] = {
    object dsl extends Http4sDsl[F]
    import dsl._

    // TODO Obtain Id[User] from AuthMiddleware
    val userId = Id.generate[User]

    HttpRoutes.of[F] {
      case req @ POST -> Root =>
        req.as[CreateCopyRequestRequestBody].flatMap { requestBody =>
          service.create(requestBody.convertTo[Id[Copy]])(userId).flatMap {
            withSuccessfulAuthorization {
              withNoServiceError { copyRequest =>
                Ok(copyRequest.convertTo[CopyRequestResponseBody])
              }
            }
          }
        }
    }
  }

}

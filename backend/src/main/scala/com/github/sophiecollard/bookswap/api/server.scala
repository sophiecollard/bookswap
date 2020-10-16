package com.github.sophiecollard.bookswap.api

import cats.effect.ConcurrentEffect
import com.github.sophiecollard.bookswap.config.ServerConfig
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits._
import org.http4s.server.jetty.JettyBuilder
import org.http4s.server.{DefaultServiceErrorHandler, Router, ServerBuilder}
import org.http4s.{Http, HttpRoutes}

import scala.concurrent.duration._

object server {

  def create[F[_]: ConcurrentEffect](
    authorEndpoints: HttpRoutes[F],
    editionEndpoints: HttpRoutes[F],
    copyEndpoints: HttpRoutes[F],
    copyRequestEndpoints: HttpRoutes[F]
  )(
    config: ServerConfig
  ): ServerBuilder[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl._

    val router: Http[F, F] = Router[F](
      "/live" -> HttpRoutes.of[F] { case GET -> Root => Ok() },
      "/v1/authors" -> authorEndpoints,
      "/v1/editions" -> editionEndpoints,
      "/v1/copies" -> copyEndpoints,
      "/v1/requests" -> copyRequestEndpoints
    ).orNotFound

    JettyBuilder[F]
      .bindHttp(config.port, config.host)
      .withIdleTimeout(1.minute)
      .withServiceErrorHandler(DefaultServiceErrorHandler)
      .mountService(
        HttpRoutes.of { case request => router.run(request) },
        prefix = ""
      )
      .withBanner(Nil) // TODO check what this does
  }

}

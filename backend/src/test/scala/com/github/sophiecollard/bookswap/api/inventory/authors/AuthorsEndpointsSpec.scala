package com.github.sophiecollard.bookswap.api.inventory.authors

import java.time.ZoneId

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import cats.implicits._
import cats.{~>, Id => CatsId}
import com.github.sophiecollard.bookswap.api.syntax._
import com.github.sophiecollard.bookswap.authorization.instances.{byActiveStatus, byAdminStatus}
import com.github.sophiecollard.bookswap.domain.inventory.Author
import com.github.sophiecollard.bookswap.domain.shared.{Id, Name}
import com.github.sophiecollard.bookswap.domain.user.{Email, Password, User, UserStatus}
import com.github.sophiecollard.bookswap.fixtures.repositories.inventory.TestAuthorsRepository
import com.github.sophiecollard.bookswap.fixtures.repositories.user.TestUsersRepository
import com.github.sophiecollard.bookswap.fixtures.services.users.TestAuthenticationService
import com.github.sophiecollard.bookswap.services.inventory.authors.AuthorsService
import com.github.sophiecollard.bookswap.services.users.AuthenticationService
import com.github.sophiecollard.bookswap.specsyntax._
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.implicits._
import org.http4s.server.AuthMiddleware
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AuthorsEndpointsSpec extends AnyWordSpec with Matchers {

  "The 'GET /{author_id}' endpoint" should {
    "return an author" in new WithAuthor {
      private val uri = uri"/" / authorId.value.toString
      private val request = Request[IO](Method.GET, uri)
      private val response = authorsEndpoints.orNotFound.run(request).unsafeRunSync()

      response.status shouldBe Status.Ok
      response.withBodyAs[AuthorResponseBody] {
        _ shouldBe authorResponseBody
      }
    }
  }

  "The 'POST /' endpoint" should {
    "create a new author" in new WithTestData {
      private val uri = uri"/"
      private val requestBody = CreateAuthorRequestBody(Name("Leo Tolstoy"))
      private val entity = circeEntityEncoder[IO, CreateAuthorRequestBody].toEntity(requestBody)
      private val request = Request[IO](Method.POST, uri, body = entity.body)
      private val response = authorsEndpoints.orNotFound.run(request).unsafeRunSync()

      response.status shouldBe Status.Created
      response.withBodyAs[AuthorResponseBody] { responseBody =>
        responseBody.name shouldBe requestBody.name
        assert(authorExists(responseBody.id))
      }
    }
  }

  "The 'DELETE /{author_id}' endpoint" should {
    "delete an author" in new WithAuthor {
      private val uri = uri"/" / authorId.value.toString
      private val request = Request[IO](Method.DELETE, uri)
      private val response = authorsEndpoints.orNotFound.run(request).unsafeRunSync()

      response.status shouldBe Status.Ok

      assert(authorDoesNotExist(authorId))
    }
  }

  trait WithBasicSetup {
    private val usersRepository = TestUsersRepository.create[IO]
    val authorsRepository = TestAuthorsRepository.create[CatsId]

    implicit val zoneId: ZoneId = ZoneId.of("UTC")

    private val transactor = new ~>[CatsId, IO] {
      override def apply[T](f : CatsId[T]): IO[T] =
        f.pure[IO]
    }

    private val authorsService: AuthorsService[IO] =
      AuthorsService.create(
        authorizationByActiveStatus = byActiveStatus(usersRepository),
        authorizationByAdminStatus = byAdminStatus(usersRepository),
        authorsRepository,
        transactor
      )

    val adminUserId = Id.generate[User]
    private val adminUser = User(adminUserId, Name("AdminUser"), UserStatus.Admin)
    usersRepository.create(adminUser)

    private val authenticationService: AuthenticationService[IO] =
      TestAuthenticationService.create(adminUserId)

    private val authMiddleware: AuthMiddleware[IO, Id[User]] =
      AuthMiddleware(
        Kleisli.liftF {
          val dummyEmail = Email.unsafeApply("vasily.grossman@bookswap.org")
          val dummyPassword = new Password("123456")
          OptionT(authenticationService.authenticate(dummyEmail, dummyPassword))
        }
      )

    val authorsEndpoints: HttpRoutes[IO] =
      AuthorsEndpoints.create(authMiddleware, authorsService)

    def authorExists(id: Id[Author]): Boolean =
      authorsRepository.get(id).isDefined

    def authorDoesNotExist(id: Id[Author]): Boolean =
      authorsRepository.get(id).isEmpty
  }

  trait WithTestData extends WithBasicSetup {
    val authorId = Id.generate[Author]
    val author = Author(authorId, Name("Vasily Grossman"))
    val authorResponseBody = author.convertTo[AuthorResponseBody]
  }

  trait WithAuthor extends WithTestData {
    authorsRepository.create(author)
  }

}

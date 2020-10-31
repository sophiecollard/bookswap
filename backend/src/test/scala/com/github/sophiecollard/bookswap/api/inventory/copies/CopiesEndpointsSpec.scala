package com.github.sophiecollard.bookswap.api.inventory.copies

import java.time.{LocalDateTime, ZoneId}

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import cats.implicits._
import cats.{~>, Id => CatsId}
import com.github.sophiecollard.bookswap.api.syntax._
import com.github.sophiecollard.bookswap.authorization.instances.byActiveStatus
import com.github.sophiecollard.bookswap.domain.inventory.{Condition, Copy, CopyStatus, ISBN}
import com.github.sophiecollard.bookswap.domain.shared.{Id, Name}
import com.github.sophiecollard.bookswap.domain.user.{Email, Password, User, UserStatus}
import com.github.sophiecollard.bookswap.fixtures.repositories.inventory.TestCopiesRepository
import com.github.sophiecollard.bookswap.fixtures.repositories.transaction.TestCopyRequestsRepository
import com.github.sophiecollard.bookswap.fixtures.repositories.user.TestUsersRepository
import com.github.sophiecollard.bookswap.fixtures.services.users.TestAuthenticationService
import com.github.sophiecollard.bookswap.services.inventory.copies.{CopiesService, authorization}
import com.github.sophiecollard.bookswap.services.users.AuthenticationService
import com.github.sophiecollard.bookswap.specsyntax.ResponseSyntax
import org.http4s._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.implicits._
import org.http4s.server.AuthMiddleware
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CopiesEndpointsSpec extends AnyWordSpec with Matchers {

  "The 'GET ?isbn={isbn}' endpoint" should {
    "list copies for a given ISBN" in new WithCopy {
      private val uri = uri"/".withQueryParam("isbn", isbn.value)
      private val request = Request[IO](Method.GET, uri)
      private val response = copiesEndpoints.orNotFound.run(request).unsafeRunSync()

      response.status shouldBe Status.Ok
      response.withBodyAs[List[CopyResponseBody]] {
        _ should contain theSameElementsAs List(copyResponseBody)
      }
    }
  }

  "The 'GET ?offered_by={user_id}' endpoint" should {
    "list copies offered by a given user" in new WithCopy {
      private val uri = uri"/".withQueryParam("offered_by", copyOwnerId.value.toString)
      private val request = Request[IO](Method.GET, uri)
      private val response = copiesEndpoints.orNotFound.run(request).unsafeRunSync()

      response.status shouldBe Status.Ok
      response.withBodyAs[List[CopyResponseBody]] {
        _ should contain theSameElementsAs List(copyResponseBody)
      }
    }
  }

  "The 'GET /{copy_id}' endpoint" should {
    "return a copy" in new WithCopy {
      private val uri = uri"/" / copyId.value.toString
      private val request = Request[IO](Method.GET, uri)
      private val response = copiesEndpoints.orNotFound.run(request).unsafeRunSync()

      response.status shouldBe Status.Ok
      response.withBodyAs[CopyResponseBody] {
        _ shouldBe copyResponseBody
      }
    }
  }

  "The 'POST /' endpoint" should {
    "create a new copy" in new WithBasicSetup {
      private val uri = uri"/"
      private val requestBody = CreateCopyRequestBody(isbn, initialCopyCondition)
      private val entity = circeEntityEncoder[IO, CreateCopyRequestBody].toEntity(requestBody)
      private val request = Request[IO](Method.POST, uri, body = entity.body)
      private val response = copiesEndpoints.orNotFound.run(request).unsafeRunSync()

      response.status shouldBe Status.Created
      response.withBodyAs[CopyResponseBody] { responseBody =>
        responseBody.isbn shouldBe isbn
        responseBody.offeredBy shouldBe copyOwnerId
        responseBody.condition shouldBe initialCopyCondition
        responseBody.status shouldBe initialCopyStatus
        assert(copyExists(responseBody.id))
      }
    }
  }

  "The 'PUT /{copy_id}' endpoint" should {
    "update a copy's condition" in new WithCopy {
      private val uri = uri"/" / copyId.value.toString
      private val requestBody = UpdateCopyRequestBody(Condition.SomeSignsOfUse)
      private val entity = circeEntityEncoder[IO, UpdateCopyRequestBody].toEntity(requestBody)
      private val request = Request[IO](Method.PUT, uri, body = entity.body)
      private val response = copiesEndpoints.orNotFound.run(request).unsafeRunSync()

      response.status shouldBe Status.Ok
      response.withBodyAs[CopyResponseBody] { responseBody =>
        responseBody.condition shouldBe Condition.SomeSignsOfUse
      }

      assert(copyConditionIs(copyId, Condition.SomeSignsOfUse))
    }
  }

  "The 'DELETE /{copy_id}' endpoint" should {
    "withdraw a copy" in new WithCopy {
      private val uri = uri"/" / copyId.value.toString
      private val request = Request[IO](Method.DELETE, uri)
      private val response = copiesEndpoints.orNotFound.run(request).unsafeRunSync()

      response.status shouldBe Status.Ok

      assert(copyIsWithdrawn(copyId))
    }
  }

  trait WithBasicSetup {
    val usersRepository = new TestUsersRepository
    val copiesRepository = new TestCopiesRepository
    val copyRequestsRepository = new TestCopyRequestsRepository

    implicit val zoneId: ZoneId = ZoneId.of("UTC")

    private val transactor = new ~>[CatsId, IO] {
      override def apply[T](f : CatsId[T]): IO[T] =
        f.pure[IO]
    }

    val copiesService: CopiesService[IO] =
      CopiesService.create(
        authorizationByActiveStatus = byActiveStatus(usersRepository),
        authorizationByCopyOwner = authorization.byCopyOwner(copiesRepository),
        copiesRepository,
        copyRequestsRepository,
        transactor
      )

    val (copyOwnerId, copyId) = (Id.generate[User], Id.generate[Copy])
    val (isbn, initialCopyCondition, initialCopyStatus) = (ISBN.unsafeApply("9781784875435"), Condition.BrandNew, CopyStatus.available)

    private val copyOwner = User(copyOwnerId, Name("CopyOwner"), UserStatus.Active)
    usersRepository.create(copyOwner)

    val copy = Copy(
      id = copyId,
      isbn,
      offeredBy = copyOwnerId,
      offeredOn = LocalDateTime.of(2019, 7, 13, 13, 0, 0),
      condition = initialCopyCondition,
      status = initialCopyStatus
    )

    val copyResponseBody = copy.convertTo[CopyResponseBody]

    val authenticationService: AuthenticationService[IO] =
      TestAuthenticationService.create(copyOwnerId)

    val authMiddleware: AuthMiddleware[IO, Id[User]] =
      AuthMiddleware(
        Kleisli.liftF {
          val dummyEmail = Email.unsafeApply("miguel.de.cervantes@bookswap.com")
          val dummyPassword = new Password("12345")
          OptionT(authenticationService.authenticate(dummyEmail, dummyPassword))
        }
      )

    val copiesEndpoints: HttpRoutes[IO] =
      CopiesEndpoints.create(
        authMiddleware,
        copiesService
      )

    def copyExists(id: Id[Copy]): Boolean =
      copiesRepository.get(id).isDefined

    def copyConditionIs(id: Id[Copy], condition: Condition): Boolean =
      copiesRepository.get(id).exists(_.condition == condition)

    def copyIsWithdrawn(id: Id[Copy]): Boolean =
      copiesRepository.get(id).exists(_.status == CopyStatus.Withdrawn)
  }

  trait WithCopy extends WithBasicSetup {
    copiesRepository.create(copy)
  }

}

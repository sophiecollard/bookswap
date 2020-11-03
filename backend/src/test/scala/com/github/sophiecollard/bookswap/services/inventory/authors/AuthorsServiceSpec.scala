package com.github.sophiecollard.bookswap.services.inventory.authors

import java.time.ZoneId

import cats.{~>, Id => CatsId}
import com.github.sophiecollard.bookswap.authorization.error.AuthorizationError.{NotAnActiveUser, NotAnAdmin}
import com.github.sophiecollard.bookswap.authorization.instances
import com.github.sophiecollard.bookswap.domain.inventory.Author
import com.github.sophiecollard.bookswap.domain.shared.{Id, Name}
import com.github.sophiecollard.bookswap.domain.user.{User, UserStatus}
import com.github.sophiecollard.bookswap.fixtures.repositories.inventory.TestAuthorsRepository
import com.github.sophiecollard.bookswap.fixtures.repositories.user.TestUsersRepository
import com.github.sophiecollard.bookswap.services.error.ServiceError.ResourceNotFound
import com.github.sophiecollard.bookswap.specsyntax._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AuthorsServiceSpec extends AnyWordSpec with Matchers {

  "The 'get' method" should {
    "return an author" in new WithAuthor {
      withRight(authorsService.get(authorId)) {
        _ shouldBe author
      }
    }

    "return an error if the author is not found" in new WithAuthor {
      withLeft(authorsService.get(otherAuthorId)) {
        _ shouldBe ResourceNotFound("Author", otherAuthorId)
      }
    }
  }

  "The 'create' method" should {
    "deny any request from a user that is pending verification or banned" in new WithBasicSetup {
      val (unverifiedUserId, bannedUserId) = (Id.generate[User], Id.generate[User])

      usersRepository.create(User(id = unverifiedUserId, name = Name("UnverifiedUser"), status = UserStatus.PendingVerification))
      usersRepository.create(User(id = bannedUserId, name = Name("BannedUser"), status = UserStatus.Banned))

      withFailedAuthorization(authorsService.create(authorName)(unverifiedUserId)) {
        _ shouldBe NotAnActiveUser(unverifiedUserId)
      }

      withFailedAuthorization(authorsService.create(authorName)(bannedUserId)) {
        _ shouldBe NotAnActiveUser(bannedUserId)
      }
    }

    "create a new author" in new WithBasicSetup {
      withSuccessfulAuthorization(authorsService.create(authorName)(activeUserId)) {
        withNoServiceError { returnedAuthor =>
          returnedAuthor.name shouldBe authorName

          withSome(authorsRepository.get(returnedAuthor.id)) { createdAuthor =>
            createdAuthor shouldBe returnedAuthor
          }
        }
      }
    }
  }

  "The 'delete' method" should {
    "deny any request from a user that is not an admin" in new WithAuthor {
      withFailedAuthorization(authorsService.delete(authorId)(activeUserId)) {
        _ shouldBe NotAnAdmin(activeUserId)
      }
    }

    "delete an author" in new WithAuthor {
      withSuccessfulAuthorization(authorsService.delete(authorId)(adminUserId)) {
        withNoServiceError { _ =>
          withNone(authorsRepository.get(authorId)) {
            succeed
          }
        }
      }
    }

    "return an error if the author is not found" in new WithAuthor {
      withSuccessfulAuthorization(authorsService.delete(otherAuthorId)(adminUserId)) {
        withServiceError {
          _ shouldBe ResourceNotFound("Author", otherAuthorId)
        }
      }
    }
  }

  trait WithBasicSetup {
    val usersRepository = TestUsersRepository.create[CatsId]
    val authorsRepository = TestAuthorsRepository.create[CatsId]

    implicit val zoneId: ZoneId = ZoneId.of("UTC")

    private val catsIdTransactor = new ~>[CatsId, CatsId] {
      override def apply[T](f : CatsId[T]): CatsId[T] =
        f
    }

    val authorsService: AuthorsService[CatsId] =
      AuthorsService.create(
        authorizationByActiveStatus = instances.byActiveStatus(usersRepository),
        authorizationByAdminStatus = instances.byAdminStatus(usersRepository),
        authorsRepository,
        catsIdTransactor
      )

    val (authorId, otherAuthorId) = (Id.generate[Author], Id.generate[Author])
    val (activeUserId, adminUserId) = (Id.generate[User], Id.generate[User])

    usersRepository.create(User(id = activeUserId, name = Name("ActiveUser"), status = UserStatus.Active))
    usersRepository.create(User(id = adminUserId, name = Name("AdminUser"), status = UserStatus.Admin))

    val authorName = Name[Author]("China Miéville")
    val author = Author(authorId, authorName)
  }

  trait WithAuthor extends WithBasicSetup {
    authorsRepository.create(author)
  }

}

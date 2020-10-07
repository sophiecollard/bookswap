package com.github.sophiecollard.bookswap.services.inventory.author

import java.time.ZoneId

import cats.{~>, Id => CatsId}
import com.github.sophiecollard.bookswap.authorization.error.AuthorizationError.{NotAnActiveUser, NotAnAdmin}
import com.github.sophiecollard.bookswap.authorization.instances
import com.github.sophiecollard.bookswap.domain.inventory.Author
import com.github.sophiecollard.bookswap.domain.shared.{Id, Name}
import com.github.sophiecollard.bookswap.domain.user.{User, UserStatus}
import com.github.sophiecollard.bookswap.fixtures.repositories.inventory.TestAuthorRepository
import com.github.sophiecollard.bookswap.fixtures.repositories.user.TestUserRepository
import com.github.sophiecollard.bookswap.services.error.ServiceError.ResourceNotFound
import com.github.sophiecollard.bookswap.specsyntax._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AuthorServiceSpec extends AnyWordSpec with Matchers {

  "The 'get' method" should {
    "return an author" in new WithAuthor {
      withRight(authorService.get(authorId)) {
        _ shouldBe author
      }
    }

    "return an error if the author is not found" in new WithAuthor {
      withLeft(authorService.get(otherAuthorId)) {
        _ shouldBe ResourceNotFound("Author", otherAuthorId)
      }
    }
  }

  "The 'create' method" should {
    "deny any request from a user that is pending verification, banned or deleted" in new WithBasicSetup {
      val (unverifiedUserId, bannedUserId, deletedUserId) = (Id.generate[User], Id.generate[User], Id.generate[User])

      userRepository.create(User(id = unverifiedUserId, name = Name("UnverifiedUser"), status = UserStatus.PendingVerification))
      userRepository.create(User(id = bannedUserId, name = Name("BannedUser"), status = UserStatus.Banned))
      userRepository.create(User(id = deletedUserId, name = Name("DeletedUser"), status = UserStatus.Deleted))

      withFailedAuthorization(authorService.create(authorName)(unverifiedUserId)) {
        _ shouldBe NotAnActiveUser(unverifiedUserId)
      }

      withFailedAuthorization(authorService.create(authorName)(bannedUserId)) {
        _ shouldBe NotAnActiveUser(bannedUserId)
      }

      withFailedAuthorization(authorService.create(authorName)(deletedUserId)) {
        _ shouldBe NotAnActiveUser(deletedUserId)
      }
    }

    "create a new author" in new WithBasicSetup {
      withSuccessfulAuthorization(authorService.create(authorName)(activeUserId)) {
        withNoServiceError { returnedAuthor =>
          returnedAuthor.name shouldBe authorName

          withSome(authorRepository.get(returnedAuthor.id)) { createdAuthor =>
            createdAuthor shouldBe returnedAuthor
          }
        }
      }
    }
  }

  "The 'delete' method" should {
    "deny any request from a user that is not an admin" in new WithAuthor {
      withFailedAuthorization(authorService.delete(authorId)(activeUserId)) {
        _ shouldBe NotAnAdmin(activeUserId)
      }
    }

    "delete an author" in new WithAuthor {
      withSuccessfulAuthorization(authorService.delete(authorId)(adminUserId)) {
        withNoServiceError { _ =>
          withNone(authorRepository.get(authorId)) {
            succeed
          }
        }
      }
    }

    "return an error if the author is not found" in new WithAuthor {
      withSuccessfulAuthorization(authorService.delete(otherAuthorId)(adminUserId)) {
        withServiceError {
          _ shouldBe ResourceNotFound("Author", otherAuthorId)
        }
      }
    }
  }

  trait WithBasicSetup {
    val userRepository = new TestUserRepository
    val authorRepository = new TestAuthorRepository

    implicit val zoneId: ZoneId = ZoneId.of("UTC")

    private val catsIdTransactor = new ~>[CatsId, CatsId] {
      override def apply[T](f : CatsId[T]): CatsId[T] =
        f
    }

    val authorService: AuthorService[CatsId] =
      AuthorService.create(
        authorizationByActiveStatus = instances.byActiveStatus(userRepository),
        authorizationByAdminStatus = instances.byAdminStatus(userRepository),
        authorRepository,
        catsIdTransactor
      )

    val (authorId, otherAuthorId) = (Id.generate[Author], Id.generate[Author])
    val (activeUserId, adminUserId) = (Id.generate[User], Id.generate[User])

    userRepository.create(User(id = activeUserId, name = Name("ActiveUser"), status = UserStatus.Active))
    userRepository.create(User(id = adminUserId, name = Name("AdminUser"), status = UserStatus.Admin))

    val authorName = Name[Author]("China Miéville")
    val author = Author(authorId, authorName)
  }

  trait WithAuthor extends WithBasicSetup {
    authorRepository.create(author)
  }

}

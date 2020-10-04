package com.github.sophiecollard.bookswap.services.inventory.edition

import java.time.{LocalDate, ZoneId}

import cats.data.NonEmptyList
import cats.{~>, Id => CatsId}
import com.github.sophiecollard.bookswap.domain.inventory.{Author, Edition, EditionDetails, ISBN, Publisher, Title}
import com.github.sophiecollard.bookswap.domain.shared.{Id, Name}
import com.github.sophiecollard.bookswap.domain.user.{User, UserStatus}
import com.github.sophiecollard.bookswap.error.AuthorizationError.{NotAnActiveUser, NotAnAdmin}
import com.github.sophiecollard.bookswap.error.ServiceError.{EditionNotFound, FailedToCreateEdition, FailedToDeleteEdition, FailedToUpdateEdition}
import com.github.sophiecollard.bookswap.fixtures.repositories.inventory.TestEditionRepository
import com.github.sophiecollard.bookswap.fixtures.repositories.user.TestUserRepository
import com.github.sophiecollard.bookswap.services.authorization.Instances
import com.github.sophiecollard.bookswap.services.specsyntax._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EditionServiceSpec extends AnyWordSpec with Matchers {

  "The 'get' method" should {
    "return an edition" in new WithEdition {
      withRight(editionService.get(isbn)) {
        _ shouldBe edition
      }
    }

    "return an error if the edition is not found" in new WithEdition {
      withLeft(editionService.get(otherIsbn)) {
        _ shouldBe EditionNotFound(otherIsbn)
      }
    }
  }

  "The 'create' method" should {
    "deny any request from a user that is pending verification, banned or deleted" in new WithBasicSetup {
      withFailedAuthorization(editionService.create(edition)(unverifiedUserId)) {
        _ shouldBe NotAnActiveUser(unverifiedUserId)
      }

      withFailedAuthorization(editionService.create(edition)(bannedUserId)) {
        _ shouldBe NotAnActiveUser(bannedUserId)
      }

      withFailedAuthorization(editionService.create(edition)(deletedUserId)) {
        _ shouldBe NotAnActiveUser(deletedUserId)
      }
    }

    "create a new edition" in new WithBasicSetup {
      withSuccessfulAuthorization(editionService.create(edition)(activeUserId)) {
        withNoServiceError { returnedEdition =>
          returnedEdition shouldBe edition

          withSome(editionRepository.get(isbn)) { createdEdition =>
            createdEdition shouldBe edition
          }
        }
      }
    }

    "return an error if the isbn already exists" in new WithEdition {
      withSuccessfulAuthorization(editionService.create(edition)(activeUserId)) {
        withServiceError {
          _ shouldBe FailedToCreateEdition(isbn)
        }
      }
    }
  }

  "The 'update' method" should {
    "deny any request from a user that is pending verification, banned or deleted" in new WithBasicSetup {
      withFailedAuthorization(editionService.update(isbn, editionDetails)(unverifiedUserId)) {
        _ shouldBe NotAnActiveUser(unverifiedUserId)
      }

      withFailedAuthorization(editionService.update(isbn, editionDetails)(bannedUserId)) {
        _ shouldBe NotAnActiveUser(bannedUserId)
      }

      withFailedAuthorization(editionService.update(isbn, editionDetails)(deletedUserId)) {
        _ shouldBe NotAnActiveUser(deletedUserId)
      }
    }

    "update an edition" in new WithEdition {
      withSuccessfulAuthorization(editionService.update(isbn, editionDetails)(activeUserId)) {
        withNoServiceError { returnedEdition =>
          assert(returnedEdition.title == editionDetails.title)
          assert(returnedEdition.authorIds == editionDetails.authorIds)
          assert(returnedEdition.publisherId == editionDetails.publisherId)
          assert(returnedEdition.publicationDate == editionDetails.publicationDate)

          withSome(editionRepository.get(isbn)) { updatedEdition =>
            assert(updatedEdition.title == editionDetails.title)
            assert(updatedEdition.authorIds == editionDetails.authorIds)
            assert(updatedEdition.publisherId == editionDetails.publisherId)
            assert(updatedEdition.publicationDate == editionDetails.publicationDate)
          }
        }
      }
    }

    "return an error if the edition is not found" in new WithEdition {
      withSuccessfulAuthorization(editionService.update(otherIsbn, editionDetails)(activeUserId)) {
        withServiceError {
          _ shouldBe FailedToUpdateEdition(otherIsbn)
        }
      }
    }
  }

  "The 'delete' method" should {
    "deny any request from a user that is not an admin" in new WithEdition {
      withFailedAuthorization(editionService.delete(isbn)(activeUserId)) {
        _ shouldBe NotAnAdmin(activeUserId)
      }
    }

    "delete an edition" in new WithEdition {
      withSuccessfulAuthorization(editionService.delete(isbn)(adminUserId)) {
        withNoServiceError { _ =>
          withNone(editionRepository.get(isbn)) {
            succeed
          }
        }
      }
    }

    "return an error if the edition is not found" in new WithEdition {
      withSuccessfulAuthorization(editionService.delete(otherIsbn)(adminUserId)) {
        withServiceError {
          _ shouldBe FailedToDeleteEdition(otherIsbn)
        }
      }
    }
  }

  trait WithBasicSetup {
    val userRepository = new TestUserRepository
    val editionRepository = new TestEditionRepository

    implicit val zoneId: ZoneId = ZoneId.of("UTC")

    private val catsIdTransactor = new ~>[CatsId, CatsId] {
      override def apply[T](f : CatsId[T]): CatsId[T] =
        f
    }

    val editionService: EditionService[CatsId] =
      EditionService.create(
        authorizationByActiveStatus = Instances.byActiveStatus(userRepository),
        authorizationByAdminStatus = Instances.byAdminStatus(userRepository),
        editionRepository,
        catsIdTransactor
      )

    val (activeUserId, adminUserId) = (Id.generate[User], Id.generate[User])
    val (unverifiedUserId, bannedUserId, deletedUserId) = (Id.generate[User], Id.generate[User], Id.generate[User])
    val (isbn, otherIsbn) = (ISBN.unvalidated("9781784875435"), ISBN.unvalidated("9780007232161"))

    userRepository.create(User(id = activeUserId, name = Name("ActiveUser"), status = UserStatus.Active))
    userRepository.create(User(id = adminUserId, name = Name("AdminUser"), status = UserStatus.Admin))
    userRepository.create(User(id = unverifiedUserId, name = Name("UnverifiedUser"), status = UserStatus.PendingVerification))
    userRepository.create(User(id = bannedUserId, name = Name("BannedUser"), status = UserStatus.Banned))
    userRepository.create(User(id = deletedUserId, name = Name("DeletedUser"), status = UserStatus.Deleted))

    val edition = Edition(
      isbn,
      title = Title("The Makioka Sisters"),
      authorIds = NonEmptyList.of(Id.generate[Author]),
      publisherId = None,
      publicationDate = None
    )

    val editionDetails = EditionDetails(
      title = Title("The Makioka Sisters: Vintage Classics Japanese Series"),
      authorIds = edition.authorIds,
      publisherId = Some(Id.generate[Publisher]),
      publicationDate = Some(LocalDate.of(2019, 10, 3))
    )
  }

  trait WithEdition extends WithBasicSetup {
    editionRepository.create(edition)
  }

}

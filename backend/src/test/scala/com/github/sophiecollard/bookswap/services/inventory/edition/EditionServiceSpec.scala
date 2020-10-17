package com.github.sophiecollard.bookswap.services.inventory.edition

import java.time.{LocalDate, LocalDateTime, ZoneId}

import cats.data.NonEmptyList
import cats.{~>, Id => CatsId}
import com.github.sophiecollard.bookswap.authorization.error.AuthorizationError.{NotAnActiveUser, NotAnAdmin}
import com.github.sophiecollard.bookswap.authorization.instances
import com.github.sophiecollard.bookswap.domain.inventory._
import com.github.sophiecollard.bookswap.domain.shared.MaybeUpdate.{NoUpdate, Update}
import com.github.sophiecollard.bookswap.domain.shared.{Id, Name}
import com.github.sophiecollard.bookswap.domain.user.{User, UserStatus}
import com.github.sophiecollard.bookswap.fixtures.repositories.inventory.{TestCopiesRepository, TestEditionsRepository}
import com.github.sophiecollard.bookswap.fixtures.repositories.user.TestUserRepository
import com.github.sophiecollard.bookswap.services.error.ServiceError.{EditionAlreadyExists, EditionNotFound, EditionStillHasCopiesOnOffer}
import com.github.sophiecollard.bookswap.specsyntax._
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
    "deny any request from a user that is pending verification or banned" in new WithBasicSetup {
      withFailedAuthorization(editionService.create(edition)(unverifiedUserId)) {
        _ shouldBe NotAnActiveUser(unverifiedUserId)
      }

      withFailedAuthorization(editionService.create(edition)(bannedUserId)) {
        _ shouldBe NotAnActiveUser(bannedUserId)
      }
    }

    "create a new edition" in new WithBasicSetup {
      withSuccessfulAuthorization(editionService.create(edition)(activeUserId)) {
        withNoServiceError { returnedEdition =>
          returnedEdition shouldBe edition

          withSome(editionsRepository.get(isbn)) { createdEdition =>
            createdEdition shouldBe edition
          }
        }
      }
    }

    "return an error if the isbn already exists" in new WithEdition {
      withSuccessfulAuthorization(editionService.create(edition)(activeUserId)) {
        withServiceError {
          _ shouldBe EditionAlreadyExists(isbn)
        }
      }
    }
  }

  "The 'update' method" should {
    "deny any request from a user that is pending verification or banned" in new WithBasicSetup {
      withFailedAuthorization(editionService.update(isbn, detailsUpdate)(unverifiedUserId)) {
        _ shouldBe NotAnActiveUser(unverifiedUserId)
      }

      withFailedAuthorization(editionService.update(isbn, detailsUpdate)(bannedUserId)) {
        _ shouldBe NotAnActiveUser(bannedUserId)
      }
    }

    "update an edition" in new WithEdition {
      val expectedResult = Edition(isbn = edition.isbn, details = edition.details.applyUpdate(detailsUpdate))

      withSuccessfulAuthorization(editionService.update(isbn, detailsUpdate)(activeUserId)) {
        withNoServiceError { returnedEdition =>
          assert(returnedEdition.title == expectedResult.title)
          assert(returnedEdition.authorIds == expectedResult.authorIds)
          assert(returnedEdition.publisherId == expectedResult.publisherId)
          assert(returnedEdition.publicationDate == expectedResult.publicationDate)

          withSome(editionsRepository.get(isbn)) { updatedEdition =>
            assert(updatedEdition.title == expectedResult.title)
            assert(updatedEdition.authorIds == expectedResult.authorIds)
            assert(updatedEdition.publisherId == expectedResult.publisherId)
            assert(updatedEdition.publicationDate == expectedResult.publicationDate)
          }
        }
      }
    }

    "return an error if the edition is not found" in new WithEdition {
      withSuccessfulAuthorization(editionService.update(otherIsbn, detailsUpdate)(activeUserId)) {
        withServiceError {
          _ shouldBe EditionNotFound(otherIsbn)
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
          withNone(editionsRepository.get(isbn)) {
            succeed
          }
        }
      }
    }

    "return an error if the edition is not found" in new WithEdition {
      withSuccessfulAuthorization(editionService.delete(otherIsbn)(adminUserId)) {
        withServiceError {
          _ shouldBe EditionNotFound(otherIsbn)
        }
      }
    }

    "return an error if the edition still has copies with status 'available'" in new WithCopyAvailable {
      withSuccessfulAuthorization(editionService.delete(isbn)(adminUserId)) {
        withServiceError {
          _ shouldBe EditionStillHasCopiesOnOffer(isbn)
        }
      }
    }

    "return an error if the edition still has copies with status 'reserved'" in new WithCopyReserved {
      withSuccessfulAuthorization(editionService.delete(isbn)(adminUserId)) {
        withServiceError {
          _ shouldBe EditionStillHasCopiesOnOffer(isbn)
        }
      }
    }
  }

  trait WithBasicSetup {
    val userRepository = new TestUserRepository
    val editionsRepository = new TestEditionsRepository
    val copiesRepository = new TestCopiesRepository

    implicit val zoneId: ZoneId = ZoneId.of("UTC")

    private val catsIdTransactor = new ~>[CatsId, CatsId] {
      override def apply[T](f : CatsId[T]): CatsId[T] =
        f
    }

    val editionService: EditionService[CatsId] =
      EditionService.create(
        authorizationByActiveStatus = instances.byActiveStatus(userRepository),
        authorizationByAdminStatus = instances.byAdminStatus(userRepository),
        editionsRepository,
        copiesRepository,
        catsIdTransactor
      )

    val (activeUserId, adminUserId) = (Id.generate[User], Id.generate[User])
    val (unverifiedUserId, bannedUserId) = (Id.generate[User], Id.generate[User])
    val (isbn, otherIsbn) = (ISBN.unsafeApply("9781784875435"), ISBN.unsafeApply("9780007232161"))

    userRepository.create(User(id = activeUserId, name = Name("ActiveUser"), status = UserStatus.Active))
    userRepository.create(User(id = adminUserId, name = Name("AdminUser"), status = UserStatus.Admin))
    userRepository.create(User(id = unverifiedUserId, name = Name("UnverifiedUser"), status = UserStatus.PendingVerification))
    userRepository.create(User(id = bannedUserId, name = Name("BannedUser"), status = UserStatus.Banned))

    val edition = Edition(
      isbn,
      title = Title("The Makioka Sisters"),
      authorIds = NonEmptyList.of(Id.generate[Author]),
      publisherId = None,
      publicationDate = None
    )

    val detailsUpdate = EditionDetailsUpdate(
      title = Update(Title("The Makioka Sisters: Vintage Classics Japanese Series")),
      authorIds = NoUpdate,
      publisherId = Update(Some(Id.generate[Publisher])),
      publicationDate = Update(Some(LocalDate.of(2019, 10, 3)))
    )
  }

  trait WithEdition extends WithBasicSetup {
    editionsRepository.create(edition)
  }

  trait WithCopyAvailable extends WithEdition {
    private val copy = Copy(
      id = Id.generate[Copy],
      isbn,
      offeredBy = Id.generate[User],
      offeredOn = LocalDateTime.of(2020, 3, 12, 0, 0, 0),
      condition = Condition.Good,
      status = CopyStatus.Available
    )

    copiesRepository.create(copy)
  }

  trait WithCopyReserved extends WithEdition {
    private val copy = Copy(
      id = Id.generate[Copy],
      isbn,
      offeredBy = Id.generate[User],
      offeredOn = LocalDateTime.of(2020, 3, 13, 0, 0, 0),
      condition = Condition.BrandNew,
      status = CopyStatus.Reserved
    )

    copiesRepository.create(copy)
  }

}

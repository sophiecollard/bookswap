package com.github.sophiecollard.bookswap.services.inventory.copy

import java.time.{LocalDateTime, ZoneId}

import cats.{~>, Id => CatsId}
import com.github.sophiecollard.bookswap.authorization.error.AuthorizationError.{NotAnActiveUser, NotTheCopyOwner}
import com.github.sophiecollard.bookswap.authorization.instances._
import com.github.sophiecollard.bookswap.domain.inventory.{Condition, Copy, CopyPagination, CopyStatus, ISBN}
import com.github.sophiecollard.bookswap.domain.shared.{Id, Name, PageSize}
import com.github.sophiecollard.bookswap.domain.transaction.{CopyRequest, RequestStatus}
import com.github.sophiecollard.bookswap.domain.user.{User, UserStatus}
import com.github.sophiecollard.bookswap.fixtures.repositories.inventory.TestCopyRepository
import com.github.sophiecollard.bookswap.fixtures.repositories.transaction.TestCopyRequestRepository
import com.github.sophiecollard.bookswap.fixtures.repositories.user.TestUserRepository
import com.github.sophiecollard.bookswap.services.error.ServiceError.ResourceNotFound
import com.github.sophiecollard.bookswap.specsyntax._
import com.github.sophiecollard.bookswap.syntax.JavaTimeSyntax.now
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CopyServiceSpec extends AnyWordSpec with Matchers {

  "The 'get' method" should {
    "return a copy" in new WithCopyAvailable {
      withRight(copyService.get(copyId)) {
        _ shouldBe copy
      }
    }

    "return an error if not found" in new WithCopyAvailable {
      val otherCopyId = Id.generate[Copy]

      withLeft(copyService.get(otherCopyId)) {
        _ shouldBe ResourceNotFound("Copy", otherCopyId)
      }
    }
  }

  "The 'listForEdition' method" should {
    "return a list of copies" in new WithCopyAvailable {
      copyService.listForEdition(copy.isbn, CopyPagination.default) shouldBe List(copy)
    }

    "not return copies with status 'swapped'" in new WithCopySwapped {
      copyService.listForEdition(copy.isbn, CopyPagination.default) shouldBe Nil
    }

    "not return copies with status 'withdrawn'" in new WithCopyWithdrawn {
      copyService.listForEdition(copy.isbn, CopyPagination.default) shouldBe Nil
    }

    "return an empty list if the page size is zero" in new WithCopyAvailable {
      val pagination = CopyPagination(LocalDateTime.now, PageSize.nil)

      copyService.listForEdition(copy.isbn, pagination) shouldBe Nil
    }

    "return an empty list if no copy matches the pagination condition(s)" in new WithCopyAvailable {
      val pagination = CopyPagination(copy.offeredOn.minusDays(1), PageSize.ten)

      copyService.listForEdition(copy.isbn, pagination) shouldBe Nil
    }
  }

  "The 'listForOwner' method" should {
    "return a list of copies" in new WithCopyAvailable {
      copyService.listForOwner(copyOwnerId, CopyPagination.default) shouldBe List(copy)
    }

    "return an empty list if the page size is zero" in new WithCopyAvailable {
      val pagination = CopyPagination(LocalDateTime.now, PageSize.nil)

      copyService.listForOwner(copyOwnerId, pagination) shouldBe Nil
    }

    "return an empty list if no copy matches the pagination condition(s)" in new WithCopyAvailable {
      val pagination = CopyPagination(copy.offeredOn.minusDays(1), PageSize.ten)

      copyService.listForOwner(copyOwnerId, pagination) shouldBe Nil
    }
  }

  "The 'create' method" should {
    "deny any request from a user that is pending verification or banned" in new WithBasicSetup {
      val (unverifiedUserId, bannedUserId) = (Id.generate[User], Id.generate[User])

      userRepository.create(User(id = unverifiedUserId, name = Name("UnverifiedUser"), status = UserStatus.PendingVerification))
      userRepository.create(User(id = bannedUserId, name = Name("BannedUser"), status = UserStatus.Banned))

      withFailedAuthorization(copyService.create(copy.isbn, copy.condition)(unverifiedUserId)) {
        _ shouldBe NotAnActiveUser(unverifiedUserId)
      }

      withFailedAuthorization(copyService.create(copy.isbn, copy.condition)(bannedUserId)) {
        _ shouldBe NotAnActiveUser(bannedUserId)
      }
    }

    "create a new request" in new WithBasicSetup {
      withSuccessfulAuthorization(copyService.create(copy.isbn, copy.condition)(copyOwnerId)) {
        withRight(_) { returnedCopy =>
          assert(returnedCopy.isbn == copy.isbn)
          assert(returnedCopy.offeredBy == copyOwnerId)
          assert(returnedCopy.condition == copy.condition)
          assert(returnedCopy.status == initialCopyStatus)

          withSome(copyRepository.get(returnedCopy.id)) { createdCopy =>
            assert(createdCopy.isbn == copy.isbn)
            assert(createdCopy.offeredBy == copyOwnerId)
            assert(createdCopy.condition == copy.condition)
            assert(createdCopy.status == initialCopyStatus)
          }
        }
      }
    }
  }

  "The 'updateCondition' method" should {
    "deny any request from a user other than the copy owner" in new WithCopyAvailable {
      withFailedAuthorization(copyService.updateCondition(copyId, Condition.SomeSignsOfUse)(requestIssuerId)) { error =>
        error shouldBe NotTheCopyOwner(requestIssuerId, copyId)
      }
    }

    "update the condition of a copy" in new WithCopyAvailable {
      val condition = Condition.SomeSignsOfUse

      withSuccessfulAuthorization(copyService.updateCondition(copyId, condition)(copyOwnerId)) {
        withNoServiceError { returnedCopy =>
          returnedCopy.condition shouldBe condition
        }
      }

      assert(copyConditionIs(copyId, condition))
    }
  }

  "The 'withdraw' method" should {
    "deny any request from a user other than the copy owner" in new WithCopyAvailable {
      withFailedAuthorization(copyService.withdraw(copyId)(requestIssuerId)) { error =>
        error shouldBe NotTheCopyOwner(requestIssuerId, copyId)
      }
    }

    "withdraw an available copy and reject all pending requests" in new WithCopyAvailable {
      withSuccessfulAuthorization(copyService.withdraw(copyId)(copyOwnerId)) {
        withNoServiceError { returnedCopy =>
          returnedCopy.status shouldBe CopyStatus.withdrawn
        }
      }

      assert(copyIsWithdrawn(copyId))
      assert(requestIsRejected(requestId))
    }

    "withdraw a reserved copy and reject all open requests" in new WithCopyReserved {
      withSuccessfulAuthorization(copyService.withdraw(copyId)(copyOwnerId)) {
        withNoServiceError { returnedCopy =>
          returnedCopy.status shouldBe CopyStatus.withdrawn
        }
      }

      assert(copyIsWithdrawn(copyId))
      assert(requestIsRejected(requestId))
    }

    "not update a swapped copy" in new WithCopySwapped {
      withSuccessfulAuthorization(copyService.withdraw(copyId)(copyOwnerId)) {
        withNoServiceError { returnedCopy =>
          returnedCopy.status shouldBe initialCopyStatus
        }
      }

      assert(copyStatusIsNotUpdated(copyId, initialCopyStatus))
    }

    "not update a withdrawn copy" in new WithCopyWithdrawn {
      withSuccessfulAuthorization(copyService.withdraw(copyId)(copyOwnerId)) {
        withNoServiceError { returnedCopy =>
          returnedCopy.status shouldBe initialCopyStatus
        }
      }

      assert(copyStatusIsNotUpdated(copyId, initialCopyStatus))
    }
  }

  trait WithBasicSetup {
    val userRepository = new TestUserRepository
    val copyRepository = new TestCopyRepository
    val copyRequestRepository = new TestCopyRequestRepository

    implicit val zoneId: ZoneId = ZoneId.of("UTC")

    private val catsIdTransactor = new ~>[CatsId, CatsId] {
      override def apply[T](f : CatsId[T]): CatsId[T] =
        f
    }

    val copyService: CopyService[CatsId] =
      CopyService.create(
        authorizationByActiveStatus = byActiveStatus(userRepository),
        authorizationByCopyOwner = authorization.byCopyOwner(copyRepository),
        copyRepository,
        copyRequestRepository,
        catsIdTransactor
      )

    val (copyOwnerId, requestIssuerId) = (Id.generate[User], Id.generate[User])
    val (copyId, requestId) = (Id.generate[Copy], Id.generate[CopyRequest])
    val (initialCopyStatus, initialRequestStatus) = (CopyStatus.available, RequestStatus.pending)

    userRepository.create(User(id = copyOwnerId, name = Name("CopyOwner"), status = UserStatus.Active))

    val copy = Copy(
      id = copyId,
      isbn = ISBN.unsafeApply("9781784875435"),
      offeredBy = copyOwnerId,
      offeredOn = LocalDateTime.of(2019, 7, 13, 13, 0, 0),
      condition = Condition.BrandNew,
      status = initialCopyStatus
    )

    val copyRequest = CopyRequest(
      id = requestId,
      copyId = copy.id,
      requestedBy = requestIssuerId,
      requestedOn = LocalDateTime.of(2019, 9, 26, 17, 0, 0),
      status = initialRequestStatus
    )

    def copyConditionIs(id: Id[Copy], condition: Condition): Boolean =
      copyRepository.get(id).exists(_.condition == condition)

    def copyIsWithdrawn(id: Id[Copy]): Boolean =
      copyRepository.get(id).exists(_.status == CopyStatus.Withdrawn)

    def copyStatusIsNotUpdated(id: Id[Copy], initialCopyStatus: CopyStatus): Boolean =
      copyRepository.get(id).exists(_.status == initialCopyStatus)

    def requestIsRejected(id: Id[CopyRequest]): Boolean =
      copyRequestRepository.get(id).exists(_.status.isRejected)
  }

  trait WithCopyAvailable extends WithBasicSetup {
    copyRepository.create(copy)
    copyRequestRepository.create(copyRequest)
  }

  trait WithCopyReserved extends WithCopyAvailable {
    override val initialCopyStatus = CopyStatus.reserved
    override val initialRequestStatus = RequestStatus.accepted(now)
    copyRepository.updateStatus(copyId, initialCopyStatus)
    copyRequestRepository.updateStatus(requestId, initialRequestStatus)
  }

  trait WithCopySwapped extends WithCopyAvailable {
    override val initialCopyStatus = CopyStatus.swapped
    override val initialRequestStatus = RequestStatus.fulfilled(now)
    copyRepository.updateStatus(copyId, initialCopyStatus)
    copyRequestRepository.updateStatus(requestId, initialRequestStatus)
  }

  trait WithCopyWithdrawn extends WithCopyAvailable {
    override val initialCopyStatus = CopyStatus.withdrawn
    override val initialRequestStatus = RequestStatus.rejected(now)
    copyRepository.updateStatus(copyId, initialCopyStatus)
    copyRequestRepository.updateStatus(requestId, initialRequestStatus)
  }

}

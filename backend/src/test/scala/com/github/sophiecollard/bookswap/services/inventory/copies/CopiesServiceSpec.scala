package com.github.sophiecollard.bookswap.services.inventory.copies

import java.time.{LocalDateTime, ZoneId}

import cats.{~>, Id => CatsId}
import com.github.sophiecollard.bookswap.authorization.error.AuthorizationError.{NotAnActiveUser, NotTheCopyOwner}
import com.github.sophiecollard.bookswap.authorization.instances._
import com.github.sophiecollard.bookswap.domain.inventory.{Condition, Copy, CopyPagination, CopyStatus, ISBN}
import com.github.sophiecollard.bookswap.domain.shared.{Id, Name, PageSize}
import com.github.sophiecollard.bookswap.domain.transaction.{CopyRequest, RequestStatus}
import com.github.sophiecollard.bookswap.domain.user.{User, UserStatus}
import com.github.sophiecollard.bookswap.fixtures.repositories.inventory.TestCopiesRepository
import com.github.sophiecollard.bookswap.fixtures.repositories.transaction.TestCopyRequestsRepository
import com.github.sophiecollard.bookswap.fixtures.repositories.user.TestUsersRepository
import com.github.sophiecollard.bookswap.services.error.ServiceError.ResourceNotFound
import com.github.sophiecollard.bookswap.specsyntax._
import com.github.sophiecollard.bookswap.syntax.JavaTimeSyntax.now
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CopiesServiceSpec extends AnyWordSpec with Matchers {

  "The 'get' method" should {
    "return a copy" in new WithCopyAvailable {
      withRight(copiesService.get(copyId)) {
        _ shouldBe copy
      }
    }

    "return an error if not found" in new WithCopyAvailable {
      val otherCopyId = Id.generate[Copy]

      withLeft(copiesService.get(otherCopyId)) {
        _ shouldBe ResourceNotFound("Copy", otherCopyId)
      }
    }
  }

  "The 'listForEdition' method" should {
    "return a list of copies" in new WithCopyAvailable {
      val pagination = CopyPagination.default
      copiesService.listForEdition(copy.isbn, pagination) shouldBe List(copy)
    }

    "not return copies with status 'swapped'" in new WithCopySwapped {
      val pagination = CopyPagination.default
      copiesService.listForEdition(copy.isbn, pagination) shouldBe Nil
    }

    "not return copies with status 'withdrawn'" in new WithCopyWithdrawn {
      val pagination = CopyPagination.default
      copiesService.listForEdition(copy.isbn, pagination) shouldBe Nil
    }

    "return an empty list if the page size is zero" in new WithCopyAvailable {
      val pagination = CopyPagination(LocalDateTime.now, PageSize.nil)
      copiesService.listForEdition(copy.isbn, pagination) shouldBe Nil
    }

    "return an empty list if no copy matches the pagination condition(s)" in new WithCopyAvailable {
      val pagination = CopyPagination(copy.offeredOn.minusDays(1), PageSize.ten)
      copiesService.listForEdition(copy.isbn, pagination) shouldBe Nil
    }
  }

  "The 'listForOwner' method" should {
    "return a list of copies" in new WithCopyAvailable {
      val pagination = CopyPagination.default
      copiesService.listForOwner(copyOwnerId, pagination) shouldBe List(copy)
    }

    "return an empty list if the page size is zero" in new WithCopyAvailable {
      val pagination = CopyPagination(LocalDateTime.now, PageSize.nil)
      copiesService.listForOwner(copyOwnerId, pagination) shouldBe Nil
    }

    "return an empty list if no copy matches the pagination condition(s)" in new WithCopyAvailable {
      val pagination = CopyPagination(copy.offeredOn.minusDays(1), PageSize.ten)
      copiesService.listForOwner(copyOwnerId, pagination) shouldBe Nil
    }
  }

  "The 'create' method" should {
    "deny any request from a user that is pending verification or banned" in new WithBasicSetup {
      val (unverifiedUserId, bannedUserId) = (Id.generate[User], Id.generate[User])

      usersRepository.create(User(id = unverifiedUserId, name = Name("UnverifiedUser"), status = UserStatus.PendingVerification))
      usersRepository.create(User(id = bannedUserId, name = Name("BannedUser"), status = UserStatus.Banned))

      withFailedAuthorization(copiesService.create(copy.isbn, copy.condition)(unverifiedUserId)) {
        _ shouldBe NotAnActiveUser(unverifiedUserId)
      }

      withFailedAuthorization(copiesService.create(copy.isbn, copy.condition)(bannedUserId)) {
        _ shouldBe NotAnActiveUser(bannedUserId)
      }
    }

    "create a new request" in new WithBasicSetup {
      withSuccessfulAuthorization(copiesService.create(copy.isbn, copy.condition)(copyOwnerId)) {
        withRight(_) { returnedCopy =>
          assert(returnedCopy.isbn == copy.isbn)
          assert(returnedCopy.offeredBy == copyOwnerId)
          assert(returnedCopy.condition == copy.condition)
          assert(returnedCopy.status == initialCopyStatus)

          withSome(copiesRepository.get(returnedCopy.id)) { createdCopy =>
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
      withFailedAuthorization(copiesService.updateCondition(copyId, Condition.SomeSignsOfUse)(requestIssuerId)) { error =>
        error shouldBe NotTheCopyOwner(requestIssuerId, copyId)
      }
    }

    "update the condition of a copy" in new WithCopyAvailable {
      val condition = Condition.SomeSignsOfUse

      withSuccessfulAuthorization(copiesService.updateCondition(copyId, condition)(copyOwnerId)) {
        withNoServiceError { returnedCopy =>
          returnedCopy.condition shouldBe condition
        }
      }

      assert(copyConditionIs(copyId, condition))
    }
  }

  "The 'withdraw' method" should {
    "deny any request from a user other than the copy owner" in new WithCopyAvailable {
      withFailedAuthorization(copiesService.withdraw(copyId)(requestIssuerId)) { error =>
        error shouldBe NotTheCopyOwner(requestIssuerId, copyId)
      }
    }

    "withdraw an available copy and reject all pending requests" in new WithCopyAvailable {
      withSuccessfulAuthorization(copiesService.withdraw(copyId)(copyOwnerId)) {
        withNoServiceError { returnedCopy =>
          returnedCopy.status shouldBe CopyStatus.withdrawn
        }
      }

      assert(copyIsWithdrawn(copyId))
      assert(requestIsRejected(requestId))
    }

    "withdraw a reserved copy and reject all open requests" in new WithCopyReserved {
      withSuccessfulAuthorization(copiesService.withdraw(copyId)(copyOwnerId)) {
        withNoServiceError { returnedCopy =>
          returnedCopy.status shouldBe CopyStatus.withdrawn
        }
      }

      assert(copyIsWithdrawn(copyId))
      assert(requestIsRejected(requestId))
    }

    "not update a swapped copy" in new WithCopySwapped {
      withSuccessfulAuthorization(copiesService.withdraw(copyId)(copyOwnerId)) {
        withNoServiceError { returnedCopy =>
          returnedCopy.status shouldBe initialCopyStatus
        }
      }

      assert(copyStatusIsNotUpdated(copyId, initialCopyStatus))
    }

    "not update a withdrawn copy" in new WithCopyWithdrawn {
      withSuccessfulAuthorization(copiesService.withdraw(copyId)(copyOwnerId)) {
        withNoServiceError { returnedCopy =>
          returnedCopy.status shouldBe initialCopyStatus
        }
      }

      assert(copyStatusIsNotUpdated(copyId, initialCopyStatus))
    }
  }

  trait WithBasicSetup {
    val usersRepository = TestUsersRepository.create[CatsId]
    val copiesRepository = TestCopiesRepository.create[CatsId]
    val copyRequestsRepository = TestCopyRequestsRepository.create[CatsId]

    implicit val zoneId: ZoneId = ZoneId.of("UTC")

    private val catsIdTransactor = new ~>[CatsId, CatsId] {
      override def apply[T](f : CatsId[T]): CatsId[T] =
        f
    }

    val copiesService: CopiesService[CatsId] =
      CopiesService.create(
        authorizationByActiveStatus = byActiveStatus(usersRepository),
        authorizationByCopyOwner = authorization.byCopyOwner(copiesRepository),
        copiesRepository,
        copyRequestsRepository,
        catsIdTransactor
      )

    val (copyOwnerId, requestIssuerId) = (Id.generate[User], Id.generate[User])
    val (copyId, requestId) = (Id.generate[Copy], Id.generate[CopyRequest])
    val (initialCopyStatus, initialRequestStatus) = (CopyStatus.available, RequestStatus.pending)

    usersRepository.create(User(id = copyOwnerId, name = Name("CopyOwner"), status = UserStatus.Active))

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
      copiesRepository.get(id).exists(_.condition == condition)

    def copyIsWithdrawn(id: Id[Copy]): Boolean =
      copiesRepository.get(id).exists(_.status == CopyStatus.Withdrawn)

    def copyStatusIsNotUpdated(id: Id[Copy], initialCopyStatus: CopyStatus): Boolean =
      copiesRepository.get(id).exists(_.status == initialCopyStatus)

    def requestIsRejected(id: Id[CopyRequest]): Boolean =
      copyRequestsRepository.get(id).exists(_.status.isRejected)
  }

  trait WithCopyAvailable extends WithBasicSetup {
    copiesRepository.create(copy)
    copyRequestsRepository.create(copyRequest)
  }

  trait WithCopyReserved extends WithCopyAvailable {
    override val initialCopyStatus = CopyStatus.reserved
    override val initialRequestStatus = RequestStatus.accepted(now)
    copiesRepository.updateStatus(copyId, initialCopyStatus)
    copyRequestsRepository.updateStatus(requestId, initialRequestStatus)
  }

  trait WithCopySwapped extends WithCopyAvailable {
    override val initialCopyStatus = CopyStatus.swapped
    override val initialRequestStatus = RequestStatus.fulfilled(now)
    copiesRepository.updateStatus(copyId, initialCopyStatus)
    copyRequestsRepository.updateStatus(requestId, initialRequestStatus)
  }

  trait WithCopyWithdrawn extends WithCopyAvailable {
    override val initialCopyStatus = CopyStatus.withdrawn
    override val initialRequestStatus = RequestStatus.rejected(now)
    copiesRepository.updateStatus(copyId, initialCopyStatus)
    copyRequestsRepository.updateStatus(requestId, initialRequestStatus)
  }

}

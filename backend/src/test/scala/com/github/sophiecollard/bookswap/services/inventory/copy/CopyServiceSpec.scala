package com.github.sophiecollard.bookswap.services.inventory.copy

import java.time.{LocalDateTime, ZoneId}

import cats.{~>, Id => CatsId}
import com.github.sophiecollard.bookswap.domain.inventory.{Condition, Copy, CopyStatus, ISBN}
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.{CopyRequest, RequestStatus}
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.error.Error.AuthorizationError.NotTheCopyOwner
import com.github.sophiecollard.bookswap.error.Error.ServiceError.ResourceNotFound
import com.github.sophiecollard.bookswap.fixtures.repositories.inventory.TestCopyRepository
import com.github.sophiecollard.bookswap.fixtures.repositories.transaction.TestCopyRequestRepository
import com.github.sophiecollard.bookswap.services.testsyntax._
import com.github.sophiecollard.bookswap.syntax.JavaTimeSyntax.now
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CopyServiceSpec extends AnyWordSpec with Matchers {

  "The 'get' method" should {
    "return a request if found" in new WithCopyAvailable {
      val result = copyService.get(copyId)

      assert(result.isRight)
      assert(result.toOption.get == copy)
    }

    "return an error if not found" in new WithCopyAvailable {
      val otherCopyId = Id.generate[Copy]
      val result = copyService.get(otherCopyId)

      assert(result.isLeft)
      assert(result.swap.toOption.get == ResourceNotFound("Copy", otherCopyId))
    }
  }

  "The 'create' method" should {
    "create a new copy request" in new WithBasicSetup {
      val result = copyService.create(copy.isbn, copy.condition)(copyOwnerId)

      assert(result.isRight)

      val returnedCopy = result.toOption.get

      assert(returnedCopy.isbn == copy.isbn)
      assert(returnedCopy.offeredBy == copyOwnerId)
      assert(returnedCopy.condition == copy.condition)
      assert(returnedCopy.status == initialCopyStatus)

      val maybeCreatedCopy = copyRepository.get(returnedCopy.id)

      assert(maybeCreatedCopy.isDefined)
      assert(maybeCreatedCopy.get.isbn == copy.isbn)
      assert(maybeCreatedCopy.get.offeredBy == copyOwnerId)
      assert(maybeCreatedCopy.get.condition == copy.condition)
      assert(maybeCreatedCopy.get.status == initialCopyStatus)
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
        _ shouldBe condition
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
        withNoServiceError {
          _ shouldBe CopyStatus.withdrawn
        }
      }

      assert(copyIsWithdrawn(copyId))
      assert(requestIsRejected(requestId))
    }

    "withdraw a reserved copy and reject all open requests" in new WithCopyReserved {
      withSuccessfulAuthorization(copyService.withdraw(copyId)(copyOwnerId)) {
        withNoServiceError {
          _ shouldBe CopyStatus.withdrawn
        }
      }

      assert(copyIsWithdrawn(copyId))
      assert(requestIsRejected(requestId))
    }

    "not update a swapped copy" in new WithCopySwapped {
      withSuccessfulAuthorization(copyService.withdraw(copyId)(copyOwnerId)) {
        withNoServiceError {
          _ shouldBe initialCopyStatus
        }
      }

      assert(copyStatusIsNotUpdated(copyId, initialCopyStatus))
    }

    "not update a withdrawn copy" in new WithCopyWithdrawn {
      withSuccessfulAuthorization(copyService.withdraw(copyId)(copyOwnerId)) {
        withNoServiceError {
          _ shouldBe initialCopyStatus
        }
      }

      assert(copyStatusIsNotUpdated(copyId, initialCopyStatus))
    }
  }

  trait WithBasicSetup {
    val copyRepository = new TestCopyRepository
    val copyRequestRepository = new TestCopyRequestRepository

    implicit val zoneId: ZoneId = ZoneId.of("UTC")

    private val catsIdTransactor = new ~>[CatsId, CatsId] {
      override def apply[T](f : CatsId[T]): CatsId[T] =
        f
    }

    val copyService: CopyService[CatsId] =
      CopyService.create(
        copyOwnerAuthorizationService = Authorization.createCopyOwnerAuthorizationService(copyRepository),
        copyRepository,
        copyRequestRepository,
        catsIdTransactor
      )

    val (copyId, requestId) = (Id.generate[Copy], Id.generate[CopyRequest])
    val (copyOwnerId, requestIssuerId) = (Id.generate[User], Id.generate[User])
    val (initialCopyStatus, initialRequestStatus) = (CopyStatus.available, RequestStatus.pending)

    val copy = Copy(
      id = copyId,
      isbn = ISBN.unvalidated("9781784875435"),
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

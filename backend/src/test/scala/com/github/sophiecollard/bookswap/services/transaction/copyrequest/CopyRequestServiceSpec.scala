package com.github.sophiecollard.bookswap.services.transaction.copyrequest

import java.time.{LocalDateTime, ZoneId}

import cats.{~>, Id => CatsId}
import com.github.sophiecollard.bookswap.domain.inventory.{Condition, Copy, CopyStatus, ISBN}
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.transaction.{CopyRequest, RequestStatus}
import com.github.sophiecollard.bookswap.domain.user.User
import com.github.sophiecollard.bookswap.error.Error.AuthorizationError.{NotTheRequestIssuer, NotTheRequestedCopyOwner}
import com.github.sophiecollard.bookswap.error.Error.ServiceError.ResourceNotFound
import com.github.sophiecollard.bookswap.fixtures.repositories.inventory.TestCopyRepository
import com.github.sophiecollard.bookswap.fixtures.repositories.transaction.TestCopyRequestRepository
import com.github.sophiecollard.bookswap.syntax.JavaTimeSyntax.now
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CopyRequestServiceSpec extends AnyWordSpec with Matchers {

  "The 'get' method" should {
    "return a request if found" in new WithRequestPending {
      val result = copyRequestService.get(requestId)

      assert(result.isRight)
      assert(result.toOption.get == copyRequest)
    }

    "return an error if not found" in new WithRequestPending {
      val otherRequestId = Id.generate[CopyRequest]
      val result = copyRequestService.get(otherRequestId)

      assert(result.isLeft)
      assert(result.swap.toOption.get == ResourceNotFound("CopyRequest", otherRequestId))
    }
  }

  "The 'create' method" should {
    "create a new copy request" in new WithBasicSetup {
      val result = copyRequestService.create(copyId)(requestIssuerId)

      assert(result.isRight)

      val returnedRequest = result.toOption.get

      assert(returnedRequest.copyId == copyId)
      assert(returnedRequest.requestedBy == requestIssuerId)
      assert(returnedRequest.status == initialRequestStatus)

      val maybeCreatedRequest = copyRequestRepository.get(returnedRequest.id)

      assert(maybeCreatedRequest.isDefined)
      assert(maybeCreatedRequest.get.copyId == copyId)
      assert(maybeCreatedRequest.get.requestedBy == requestIssuerId)
      assert(maybeCreatedRequest.get.status == initialRequestStatus)
    }
  }

  "The 'cancel' method" should {
    "deny any request from a user other than the request issuer" in new WithRequestPending {
      val authorizationResult = copyRequestService.cancel(requestId)(copyOwnerId)

      assert(authorizationResult.isFailure)
      authorizationResult.unsafeError shouldBe NotTheRequestIssuer(copyOwnerId, requestId)
    }

    "cancel a pending request" in new WithRequestPending {
      val authorizationResult = copyRequestService.cancel(requestId)(requestIssuerId)

      assert(authorizationResult.isSuccess)
      assert(authorizationResult.unsafeResult.isRight)

      val (returnedRequestStatus, returnedCopyStatus) = authorizationResult.unsafeResult.toOption.get

      assert(returnedRequestStatus.isCancelled)
      assert(returnedCopyStatus == initialCopyStatus)

      assert(requestIsCancelled(requestId))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "cancel a request on the waiting list" in new WithRequestOnWaitingList {
      val authorizationResult = copyRequestService.cancel(requestId)(requestIssuerId)

      assert(authorizationResult.isSuccess)
      assert(authorizationResult.unsafeResult.isRight)

      val (returnedRequestStatus, returnedCopyStatus) = authorizationResult.unsafeResult.toOption.get

      assert(returnedRequestStatus.isCancelled)
      assert(returnedCopyStatus == initialCopyStatus)

      assert(requestIsCancelled(requestId))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "cancel a rejected request" in new WithRequestRejected {
      val authorizationResult = copyRequestService.cancel(requestId)(requestIssuerId)

      assert(authorizationResult.isSuccess)
      assert(authorizationResult.unsafeResult.isRight)

      val (returnedRequestStatus, returnedCopyStatus) = authorizationResult.unsafeResult.toOption.get

      assert(returnedRequestStatus.isCancelled)
      assert(returnedCopyStatus == initialCopyStatus)

      assert(requestIsCancelled(requestId))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "cancel an accepted request and accept the next request on the waiting list" in
      new WithNextRequestOnWaitingList {
        val authorizationResult = copyRequestService.cancel(requestId)(requestIssuerId)

        assert(authorizationResult.isSuccess)
        assert(authorizationResult.unsafeResult.isRight)

        val (returnedRequestStatus, returnedCopyStatus) = authorizationResult.unsafeResult.toOption.get

        assert(returnedRequestStatus.isCancelled)
        assert(returnedCopyStatus == initialCopyStatus)

        assert(requestIsCancelled(requestId))
        assert(requestIsAccepted(nextRequestId))
        assert(copyIsNotUpdated(copyId, initialCopyStatus))
      }

    "cancel an accepted request and update the copy status back to 'Available' if there is no waiting list" in
      new WithRequestAccepted {
        val authorizationResult = copyRequestService.cancel(requestId)(requestIssuerId)

        assert(authorizationResult.isSuccess)
        assert(authorizationResult.unsafeResult.isRight)

        val (returnedRequestStatus, returnedCopyStatus) = authorizationResult.unsafeResult.toOption.get

        assert(returnedRequestStatus.isCancelled)
        assert(returnedCopyStatus == CopyStatus.Available)

        assert(requestIsCancelled(requestId))
        assert(copyIsAvailable(copyId))
      }

    "not update a cancelled request" in new WithRequestCancelled {
      val authorizationResult = copyRequestService.cancel(requestId)(requestIssuerId)

      assert(authorizationResult.isSuccess)
      assert(authorizationResult.unsafeResult.isRight)

      val (returnedRequestStatus, returnedCopyStatus) = authorizationResult.unsafeResult.toOption.get

      assert(returnedRequestStatus == initialRequestStatus)
      assert(returnedCopyStatus == initialCopyStatus)

      assert(requestIsNotUpdated(requestId, initialRequestStatus))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "not update a fulfilled request" in new WithRequestFulfilled {
      val authorizationResult = copyRequestService.cancel(requestId)(requestIssuerId)

      assert(authorizationResult.isSuccess)
      assert(authorizationResult.unsafeResult.isRight)

      val (returnedRequestStatus, returnedCopyStatus) = authorizationResult.unsafeResult.toOption.get

      assert(returnedRequestStatus == initialRequestStatus)
      assert(returnedCopyStatus == initialCopyStatus)

      assert(requestIsNotUpdated(requestId, initialRequestStatus))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }
  }

  "The 'accept' method" should {
    "deny any request from a user other than the copy owner" in new WithRequestPending {
      val authorizationResult = copyRequestService.accept(requestId)(requestIssuerId)

      assert(authorizationResult.isFailure)
      authorizationResult.unsafeError shouldBe NotTheRequestedCopyOwner(requestIssuerId, requestId)
    }

    "accept a pending request" in new WithRequestPending {
      val authorizationResult = copyRequestService.accept(requestId)(copyOwnerId)

      assert(authorizationResult.isSuccess)
      assert(authorizationResult.unsafeResult.isRight)

      val (returnedRequestStatus, returnedCopyStatus) = authorizationResult.unsafeResult.toOption.get

      assert(returnedRequestStatus.isAccepted)
      assert(returnedCopyStatus == CopyStatus.Reserved)

      assert(requestIsAccepted(requestId))
      assert(copyIsReserved(copyId))
    }

    "accept a pending request on the waiting list if the copy is already reserved" in new WithRequestAccepted {
      val authorizationResult = copyRequestService.accept(nextRequestId)(copyOwnerId)

      assert(authorizationResult.isSuccess)
      assert(authorizationResult.unsafeResult.isRight)

      val (returnedRequestStatus, returnedCopyStatus) = authorizationResult.unsafeResult.toOption.get

      assert(returnedRequestStatus.isOnWaitingList)
      assert(returnedCopyStatus == initialCopyStatus)

      assert(requestIsOnWaitingList(nextRequestId))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "accept a rejected request" in new WithRequestRejected {
      val authorizationResult = copyRequestService.accept(requestId)(copyOwnerId)

      assert(authorizationResult.isSuccess)
      assert(authorizationResult.unsafeResult.isRight)

      val (returnedRequestStatus, returnedCopyStatus) = authorizationResult.unsafeResult.toOption.get

      assert(returnedRequestStatus.isAccepted)
      assert(returnedCopyStatus == CopyStatus.Reserved)

      assert(requestIsAccepted(requestId))
      assert(copyIsReserved(copyId))
    }

    "not update an accepted request" in new WithRequestAccepted {
      val authorizationResult = copyRequestService.accept(requestId)(copyOwnerId)

      assert(authorizationResult.isSuccess)
      assert(authorizationResult.unsafeResult.isRight)

      val (returnedRequestStatus, returnedCopyStatus) = authorizationResult.unsafeResult.toOption.get

      assert(returnedRequestStatus == initialRequestStatus)
      assert(returnedCopyStatus == initialCopyStatus)

      assert(requestIsNotUpdated(requestId, initialRequestStatus))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "not update a request on the waiting list" in new WithRequestOnWaitingList {
      val authorizationResult = copyRequestService.accept(requestId)(copyOwnerId)

      assert(authorizationResult.isSuccess)
      assert(authorizationResult.unsafeResult.isRight)

      val (returnedRequestStatus, returnedCopyStatus) = authorizationResult.unsafeResult.toOption.get

      assert(returnedRequestStatus == initialRequestStatus)
      assert(returnedCopyStatus == initialCopyStatus)

      assert(requestIsNotUpdated(requestId, initialRequestStatus))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "not update a cancelled request" in new WithRequestCancelled {
      val authorizationResult = copyRequestService.accept(requestId)(copyOwnerId)

      assert(authorizationResult.isSuccess)
      assert(authorizationResult.unsafeResult.isRight)

      val (returnedRequestStatus, returnedCopyStatus) = authorizationResult.unsafeResult.toOption.get

      assert(returnedRequestStatus == initialRequestStatus)
      assert(returnedCopyStatus == initialCopyStatus)

      assert(requestIsNotUpdated(requestId, initialRequestStatus))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "not update a fulfilled request" in new WithRequestFulfilled {
      val authorizationResult = copyRequestService.accept(requestId)(copyOwnerId)

      assert(authorizationResult.isSuccess)
      assert(authorizationResult.unsafeResult.isRight)

      val (returnedRequestStatus, returnedCopyStatus) = authorizationResult.unsafeResult.toOption.get

      assert(returnedRequestStatus == initialRequestStatus)
      assert(returnedCopyStatus == initialCopyStatus)

      assert(requestIsNotUpdated(requestId, initialRequestStatus))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }
  }

  "The 'reject' method" should {
    "deny any request from a user other than the copy owner" in new WithRequestPending {
      val authorizationResult = copyRequestService.reject(requestId)(requestIssuerId)

      assert(authorizationResult.isFailure)
      authorizationResult.unsafeError shouldBe NotTheRequestedCopyOwner(requestIssuerId, requestId)
    }

    "reject a pending request" in new WithRequestPending {
      val authorizationResult = copyRequestService.reject(requestId)(copyOwnerId)

      assert(authorizationResult.isSuccess)
      assert(authorizationResult.unsafeResult.isRight)

      val (returnedRequestStatus, returnedCopyStatus) = authorizationResult.unsafeResult.toOption.get

      assert(returnedRequestStatus.isRejected)
      assert(returnedCopyStatus == initialCopyStatus)

      assert(requestIsRejected(requestId))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "reject a request on the waiting list" in new WithRequestOnWaitingList {
      val authorizationResult = copyRequestService.reject(requestId)(copyOwnerId)

      assert(authorizationResult.isSuccess)
      assert(authorizationResult.unsafeResult.isRight)

      val (returnedRequestStatus, returnedCopyStatus) = authorizationResult.unsafeResult.toOption.get

      assert(returnedRequestStatus.isRejected)
      assert(returnedCopyStatus == initialCopyStatus)

      assert(requestIsRejected(requestId))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "reject an accepted request and accept the next request on the waiting list" in new WithNextRequestOnWaitingList {
      val authorizationResult = copyRequestService.reject(requestId)(copyOwnerId)

      assert(authorizationResult.isSuccess)
      assert(authorizationResult.unsafeResult.isRight)

      val (returnedRequestStatus, returnedCopyStatus) = authorizationResult.unsafeResult.toOption.get

      assert(returnedRequestStatus.isRejected)
      assert(returnedCopyStatus == initialCopyStatus)

      assert(requestIsRejected(requestId))
      assert(requestIsAccepted(nextRequestId))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "reject an accepted request and update the copy status back to 'Available' if there is no waiting list" in
      new WithRequestAccepted {
        val authorizationResult = copyRequestService.reject(requestId)(copyOwnerId)

        assert(authorizationResult.isSuccess)
        assert(authorizationResult.unsafeResult.isRight)

        val (returnedRequestStatus, returnedCopyStatus) = authorizationResult.unsafeResult.toOption.get

        assert(returnedRequestStatus.isRejected)
        assert(returnedCopyStatus == CopyStatus.Available)

        assert(requestIsRejected(requestId))
        assert(copyIsAvailable(copyId))
      }

    "not update a rejected request" in new WithRequestRejected {
      val authorizationResult = copyRequestService.reject(requestId)(copyOwnerId)

      assert(authorizationResult.isSuccess)
      assert(authorizationResult.unsafeResult.isRight)

      val (returnedRequestStatus, returnedCopyStatus) = authorizationResult.unsafeResult.toOption.get

      assert(returnedRequestStatus == initialRequestStatus)
      assert(returnedCopyStatus == initialCopyStatus)

      assert(requestIsNotUpdated(requestId, initialRequestStatus))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "not update a cancelled request" in new WithRequestCancelled {
      val authorizationResult = copyRequestService.reject(requestId)(copyOwnerId)

      assert(authorizationResult.isSuccess)
      assert(authorizationResult.unsafeResult.isRight)

      val (returnedRequestStatus, returnedCopyStatus) = authorizationResult.unsafeResult.toOption.get

      assert(returnedRequestStatus == initialRequestStatus)
      assert(returnedCopyStatus == initialCopyStatus)

      assert(requestIsNotUpdated(requestId, initialRequestStatus))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "not update a fulfilled request" in new WithRequestFulfilled {
      val authorizationResult = copyRequestService.reject(requestId)(copyOwnerId)

      assert(authorizationResult.isSuccess)
      assert(authorizationResult.unsafeResult.isRight)

      val (returnedRequestStatus, returnedCopyStatus) = authorizationResult.unsafeResult.toOption.get

      assert(returnedRequestStatus == initialRequestStatus)
      assert(returnedCopyStatus == initialCopyStatus)

      assert(requestIsNotUpdated(requestId, initialRequestStatus))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }
  }

  "The 'markAsFulfilled' method" should {
    "deny any request from a user other than the copy owner" in new WithRequestAccepted {
      val authorizationResult = copyRequestService.markAsFulfilled(requestId)(requestIssuerId)

      assert(authorizationResult.isFailure)
      authorizationResult.unsafeError shouldBe NotTheRequestedCopyOwner(requestIssuerId, requestId)
    }

    "mark an accepted request as fulfilled and reject all requests still pending or on the waiting list" in
      new WithRequestAccepted {
        val authorizationResult = copyRequestService.markAsFulfilled(requestId)(copyOwnerId)

        assert(authorizationResult.isSuccess)
        assert(authorizationResult.unsafeResult.isRight)

        val (returnedRequestStatus, returnedCopyStatus) = authorizationResult.unsafeResult.toOption.get

        assert(returnedRequestStatus.isFulfilled)
        assert(returnedCopyStatus == CopyStatus.Swapped)

        assert(requestIsFulfilled(requestId))
        assert(requestIsRejected(nextRequestId))
        assert(copyIsSwapped(copyId))
      }

    "not update a pending request" in new WithRequestPending {
      val authorizationResult = copyRequestService.markAsFulfilled(requestId)(copyOwnerId)

      assert(authorizationResult.isSuccess)
      assert(authorizationResult.unsafeResult.isRight)

      val (returnedRequestStatus, returnedCopyStatus) = authorizationResult.unsafeResult.toOption.get

      assert(returnedRequestStatus == initialRequestStatus)
      assert(returnedCopyStatus == initialCopyStatus)

      assert(requestIsNotUpdated(requestId, initialRequestStatus))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "not update a request on the waiting list" in new WithRequestOnWaitingList {
      val authorizationResult = copyRequestService.markAsFulfilled(requestId)(copyOwnerId)

      assert(authorizationResult.isSuccess)
      assert(authorizationResult.unsafeResult.isRight)

      val (returnedRequestStatus, returnedCopyStatus) = authorizationResult.unsafeResult.toOption.get

      assert(returnedRequestStatus == initialRequestStatus)
      assert(returnedCopyStatus == initialCopyStatus)

      assert(requestIsNotUpdated(requestId, initialRequestStatus))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "not update a rejected request" in new WithRequestRejected {
      val authorizationResult = copyRequestService.markAsFulfilled(requestId)(copyOwnerId)

      assert(authorizationResult.isSuccess)
      assert(authorizationResult.unsafeResult.isRight)

      val (returnedRequestStatus, returnedCopyStatus) = authorizationResult.unsafeResult.toOption.get

      assert(returnedRequestStatus == initialRequestStatus)
      assert(returnedCopyStatus == initialCopyStatus)

      assert(requestIsNotUpdated(requestId, initialRequestStatus))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "not update a cancelled request" in new WithRequestCancelled {
      val authorizationResult = copyRequestService.markAsFulfilled(requestId)(copyOwnerId)

      assert(authorizationResult.isSuccess)
      assert(authorizationResult.unsafeResult.isRight)

      val (returnedRequestStatus, returnedCopyStatus) = authorizationResult.unsafeResult.toOption.get

      assert(returnedRequestStatus == initialRequestStatus)
      assert(returnedCopyStatus == initialCopyStatus)

      assert(requestIsNotUpdated(requestId, initialRequestStatus))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "not update a fulfilled request" in new WithRequestFulfilled {
      val authorizationResult = copyRequestService.markAsFulfilled(requestId)(copyOwnerId)

      assert(authorizationResult.isSuccess)
      assert(authorizationResult.unsafeResult.isRight)

      val (returnedRequestStatus, returnedCopyStatus) = authorizationResult.unsafeResult.toOption.get

      assert(returnedRequestStatus == initialRequestStatus)
      assert(returnedCopyStatus == initialCopyStatus)

      assert(requestIsNotUpdated(requestId, initialRequestStatus))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
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

    val copyRequestService: CopyRequestService[CatsId] =
      CopyRequestService.create(
        requestIssuerAuthorizationService = Authorization.createRequestIssuerAuthorizationService(copyRequestRepository),
        copyOwnerAuthorizationService = Authorization.createCopyOwnerAuthorizationService(copyRequestRepository, copyRepository),
        copyRequestRepository,
        copyRepository,
        catsIdTransactor
      )

    val (copyId, requestId, nextRequestId) = (Id.generate[Copy], Id.generate[CopyRequest], Id.generate[CopyRequest])
    val (copyOwnerId, requestIssuerId, nextRequestIssuerId) = (Id.generate[User], Id.generate[User], Id.generate[User])
    val initialCopyStatus = CopyStatus.available
    val (initialRequestStatus, initialNextRequestStatus) = (RequestStatus.pending, RequestStatus.pending)

    private val copy = Copy(
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

    val nextCopyRequest = CopyRequest(
      id = nextRequestId,
      copyId = copy.id,
      requestedBy = nextRequestIssuerId,
      requestedOn = LocalDateTime.of(2019, 9, 30, 11, 0, 0),
      status = initialNextRequestStatus
    )

    copyRepository.create(copy)

    def copyIsAvailable(id: Id[Copy]): Boolean =
      copyRepository.get(id).exists(_.status == CopyStatus.Available)

    def copyIsReserved(id: Id[Copy]): Boolean =
      copyRepository.get(id).exists(_.status == CopyStatus.Reserved)

    def copyIsSwapped(id: Id[Copy]): Boolean =
      copyRepository.get(id).exists(_.status == CopyStatus.Swapped)

    def copyIsNotUpdated(id: Id[Copy], initialCopyStatus: CopyStatus): Boolean =
      copyRepository.get(id).exists(_.status == initialCopyStatus)

    def requestIsAccepted(id: Id[CopyRequest]): Boolean =
      copyRequestRepository.get(id).exists(_.status.isAccepted)

    def requestIsOnWaitingList(id: Id[CopyRequest]): Boolean =
      copyRequestRepository.get(id).exists(_.status.isOnWaitingList)

    def requestIsRejected(id: Id[CopyRequest]): Boolean =
      copyRequestRepository.get(id).exists(_.status.isRejected)

    def requestIsFulfilled(id: Id[CopyRequest]): Boolean =
      copyRequestRepository.get(id).exists(_.status.isFulfilled)

    def requestIsCancelled(id: Id[CopyRequest]): Boolean =
      copyRequestRepository.get(id).exists(_.status.isCancelled)

    def requestIsNotUpdated(id: Id[CopyRequest], initialRequestStatus: RequestStatus): Boolean =
      copyRequestRepository.get(id).exists(_.status == initialRequestStatus)
  }

  trait WithRequestPending extends WithBasicSetup {
    copyRequestRepository.create(copyRequest)
    copyRequestRepository.create(nextCopyRequest)
  }

  trait WithRequestAccepted extends WithRequestPending {
    override val initialRequestStatus = RequestStatus.accepted(now)
    override val initialCopyStatus = CopyStatus.reserved
    copyRequestRepository.updateStatus(requestId, initialRequestStatus)
    copyRepository.updateStatus(copyId, initialCopyStatus)
  }

  trait WithNextRequestOnWaitingList extends WithRequestAccepted {
    override val initialNextRequestStatus = RequestStatus.onWaitingList(now)
    copyRequestRepository.updateStatus(nextRequestId, initialNextRequestStatus)
  }

  trait WithRequestOnWaitingList extends WithRequestPending {
    override val initialRequestStatus = RequestStatus.onWaitingList(now)
    override val initialCopyStatus = CopyStatus.reserved
    copyRequestRepository.updateStatus(requestId, initialRequestStatus)
    copyRepository.updateStatus(copyId, initialCopyStatus)
  }

  trait WithRequestRejected extends WithRequestPending {
    override val initialRequestStatus = RequestStatus.rejected(now)
    copyRequestRepository.updateStatus(requestId, initialRequestStatus)
  }

  trait WithRequestCancelled extends WithRequestPending {
    override val initialRequestStatus = RequestStatus.cancelled(now)
    copyRequestRepository.updateStatus(requestId, initialRequestStatus)
  }

  trait WithRequestFulfilled extends WithRequestPending {
    override val initialRequestStatus = RequestStatus.fulfilled(now)
    override val initialCopyStatus = CopyStatus.Swapped
    copyRequestRepository.updateStatus(requestId, initialRequestStatus)
    copyRepository.updateStatus(copyId, initialCopyStatus)
  }

}

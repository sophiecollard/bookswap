package com.github.sophiecollard.bookswap.services.transaction.copyrequest

import java.time.{LocalDateTime, ZoneId}

import cats.{~>, Id => CatsId}
import com.github.sophiecollard.bookswap.authorization.error.AuthorizationError.{NotAnActiveUser, NotTheRequestIssuer, NotTheRequestedCopyOwner}
import com.github.sophiecollard.bookswap.authorization.instances
import com.github.sophiecollard.bookswap.domain.inventory.{Condition, Copy, CopyStatus, ISBN}
import com.github.sophiecollard.bookswap.domain.shared.{Id, Name, PageSize}
import com.github.sophiecollard.bookswap.domain.transaction.{CopyRequest, CopyRequestPagination, RequestStatus}
import com.github.sophiecollard.bookswap.domain.user.{User, UserStatus}
import com.github.sophiecollard.bookswap.fixtures.repositories.inventory.TestCopiesRepository
import com.github.sophiecollard.bookswap.fixtures.repositories.transaction.TestCopyRequestsRepository
import com.github.sophiecollard.bookswap.fixtures.repositories.user.TestUserRepository
import com.github.sophiecollard.bookswap.services.error.ServiceError.ResourceNotFound
import com.github.sophiecollard.bookswap.specsyntax._
import com.github.sophiecollard.bookswap.syntax.JavaTimeSyntax.now
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class CopyRequestServiceSpec extends AnyWordSpec with Matchers {

  "The 'get' method" should {
    "return a request if found" in new WithRequestPending {
      withRight(copyRequestService.get(requestId)) {
        _ shouldBe copyRequest
      }
    }

    "return an error if not found" in new WithRequestPending {
      val otherRequestId = Id.generate[CopyRequest]

      withLeft(copyRequestService.get(otherRequestId)) {
        _ shouldBe ResourceNotFound("CopyRequest", otherRequestId)
      }
    }
  }

  "The 'listForCopy' method" should {
    "return a list of requests" in new WithRequestPending {
      val pagination = CopyRequestPagination.default
      copyRequestService.listForCopy(copyId, pagination) shouldBe List(nextCopyRequest, copyRequest)
    }

    "return an empty list if the page size is zero" in new WithRequestPending {
      val pagination = CopyRequestPagination(LocalDateTime.now, PageSize.nil)
      copyRequestService.listForCopy(copyId, pagination) shouldBe Nil
    }

    "return an empty list if no request matches the pagination condition(s)" in new WithRequestPending {
      val pagination = CopyRequestPagination(copyRequest.requestedOn.minusDays(1), PageSize.ten)
      copyRequestService.listForCopy(copyId, pagination) shouldBe Nil
    }
  }

  "The 'listForRequester' method" should {
    "return a list of requests" in new WithRequestPending {
      val pagination = CopyRequestPagination.default
      copyRequestService.listForRequester(pagination)(requestIssuerId) shouldBe List(copyRequest)
    }

    "return an empty list if the page size is zero" in new WithRequestPending {
      val pagination = CopyRequestPagination(LocalDateTime.now, PageSize.nil)
      copyRequestService.listForRequester(pagination)(requestIssuerId) shouldBe Nil
    }

    "return an empty list if no request matches the pagination condition(s)" in new WithRequestPending {
      val pagination = CopyRequestPagination(copyRequest.requestedOn.minusDays(1), PageSize.ten)
      copyRequestService.listForRequester(pagination)(requestIssuerId) shouldBe Nil
    }
  }

  "The 'create' method" should {
    "deny any request from a user that is pending verification or banned" in new WithBasicSetup {
      val (unverifiedUserId, bannedUserId) = (Id.generate[User], Id.generate[User])

      userRepository.create(User(id = unverifiedUserId, name = Name("UnverifiedUser"), status = UserStatus.PendingVerification))
      userRepository.create(User(id = bannedUserId, name = Name("BannedUser"), status = UserStatus.Banned))

      withFailedAuthorization(copyRequestService.create(copyId)(unverifiedUserId)) {
        _ shouldBe NotAnActiveUser(unverifiedUserId)
      }

      withFailedAuthorization(copyRequestService.create(copyId)(bannedUserId)) {
        _ shouldBe NotAnActiveUser(bannedUserId)
      }
    }

    "create a new copy request" in new WithBasicSetup {
      withSuccessfulAuthorization(copyRequestService.create(copyId)(requestIssuerId)) {
        withNoServiceError { returnedRequest =>
          assert(returnedRequest.copyId == copyId)
          assert(returnedRequest.requestedBy == requestIssuerId)
          assert(returnedRequest.status == initialRequestStatus)

          withSome(copyRequestsRepository.get(returnedRequest.id)) { createdRequest =>
            assert(createdRequest.copyId == copyId)
            assert(createdRequest.requestedBy == requestIssuerId)
            assert(createdRequest.status == initialRequestStatus)
          }
        }
      }
    }
  }

  "The 'cancel' method" should {
    "deny any request from a user other than the request issuer" in new WithRequestPending {
      withFailedAuthorization(copyRequestService.cancel(requestId)(copyOwnerId)) { error =>
        error shouldBe NotTheRequestIssuer(copyOwnerId, requestId)
      }
    }

    "cancel a pending request" in new WithRequestPending {
      withSuccessfulAuthorization(copyRequestService.cancel(requestId)(requestIssuerId)) {
        withNoServiceError { case (returnedRequest, returnedCopy) =>
          assert(returnedRequest.status.isCancelled)
          assert(returnedCopy.status == initialCopyStatus)
        }
      }

      assert(requestIsCancelled(requestId))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "cancel a request on the waiting list" in new WithRequestOnWaitingList {
      withSuccessfulAuthorization(copyRequestService.cancel(requestId)(requestIssuerId)) {
        withNoServiceError { case (returnedRequest, returnedCopy) =>
          assert(returnedRequest.status.isCancelled)
          assert(returnedCopy.status == initialCopyStatus)
        }
      }

      assert(requestIsCancelled(requestId))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "cancel a rejected request" in new WithRequestRejected {
      withSuccessfulAuthorization(copyRequestService.cancel(requestId)(requestIssuerId)) {
        withNoServiceError { case (returnedRequest, returnedCopy) =>
          assert(returnedRequest.status.isCancelled)
          assert(returnedCopy.status == initialCopyStatus)
        }
      }

      assert(requestIsCancelled(requestId))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "cancel an accepted request and accept the next request on the waiting list" in
      new WithNextRequestOnWaitingList {
        withSuccessfulAuthorization(copyRequestService.cancel(requestId)(requestIssuerId)) {
          withNoServiceError { case (returnedRequest, returnedCopy) =>
            assert(returnedRequest.status.isCancelled)
            assert(returnedCopy.status == initialCopyStatus)
          }
        }

        assert(requestIsCancelled(requestId))
        assert(requestIsAccepted(nextRequestId))
        assert(copyIsNotUpdated(copyId, initialCopyStatus))
      }

    "cancel an accepted request and update the copy status back to 'Available' if there is no waiting list" in
      new WithRequestAccepted {
        withSuccessfulAuthorization(copyRequestService.cancel(requestId)(requestIssuerId)) {
          withNoServiceError { case (returnedRequest, returnedCopy) =>
            assert(returnedRequest.status.isCancelled)
            assert(returnedCopy.status == CopyStatus.Available)
          }
        }

        assert(requestIsCancelled(requestId))
        assert(copyIsAvailable(copyId))
      }

    "not update a cancelled request" in new WithRequestCancelled {
      withSuccessfulAuthorization(copyRequestService.cancel(requestId)(requestIssuerId)) {
        withNoServiceError { case (returnedRequest, returnedCopy) =>
          assert(returnedRequest.status == initialRequestStatus)
          assert(returnedCopy.status == initialCopyStatus)
        }
      }

      assert(requestIsNotUpdated(requestId, initialRequestStatus))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "not update a fulfilled request" in new WithRequestFulfilled {
      withSuccessfulAuthorization(copyRequestService.cancel(requestId)(requestIssuerId)) {
        withNoServiceError { case (returnedRequest, returnedCopy) =>
          assert(returnedRequest.status == initialRequestStatus)
          assert(returnedCopy.status == initialCopyStatus)
        }
      }

      assert(requestIsNotUpdated(requestId, initialRequestStatus))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }
  }

  "The 'accept' method" should {
    "deny any request from a user other than the copy owner" in new WithRequestPending {
      withFailedAuthorization(copyRequestService.accept(requestId)(requestIssuerId)) { error =>
        error shouldBe NotTheRequestedCopyOwner(requestIssuerId, requestId)
      }
    }

    "accept a pending request" in new WithRequestPending {
      withSuccessfulAuthorization(copyRequestService.accept(requestId)(copyOwnerId)) {
        withNoServiceError { case (returnedRequest, returnedCopy) =>
          assert(returnedRequest.status.isAccepted)
          assert(returnedCopy.status == CopyStatus.Reserved)
        }
      }

      assert(requestIsAccepted(requestId))
      assert(copyIsReserved(copyId))
    }

    "accept a pending request on the waiting list if the copy is already reserved" in new WithRequestAccepted {
      withSuccessfulAuthorization(copyRequestService.accept(nextRequestId)(copyOwnerId)) {
        withNoServiceError { case (returnedRequest, returnedCopy) =>
          assert(returnedRequest.status.isOnWaitingList)
          assert(returnedCopy.status == initialCopyStatus)
        }
      }

      assert(requestIsOnWaitingList(nextRequestId))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "accept a rejected request" in new WithRequestRejected {
      withSuccessfulAuthorization(copyRequestService.accept(requestId)(copyOwnerId)) {
        withNoServiceError { case (returnedRequest, returnedCopy) =>
          assert(returnedRequest.status.isAccepted)
          assert(returnedCopy.status == CopyStatus.Reserved)
        }
      }

      assert(requestIsAccepted(requestId))
      assert(copyIsReserved(copyId))
    }

    "not update an accepted request" in new WithRequestAccepted {
      withSuccessfulAuthorization(copyRequestService.accept(requestId)(copyOwnerId)) {
        withNoServiceError { case (returnedRequest, returnedCopy) =>
          assert(returnedRequest.status == initialRequestStatus)
          assert(returnedCopy.status == initialCopyStatus)
        }
      }

      assert(requestIsNotUpdated(requestId, initialRequestStatus))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "not update a request on the waiting list" in new WithRequestOnWaitingList {
      withSuccessfulAuthorization(copyRequestService.accept(requestId)(copyOwnerId)) {
        withNoServiceError { case (returnedRequest, returnedCopy) =>
          assert(returnedRequest.status == initialRequestStatus)
          assert(returnedCopy.status == initialCopyStatus)
        }
      }

      assert(requestIsNotUpdated(requestId, initialRequestStatus))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "not update a cancelled request" in new WithRequestCancelled {
      withSuccessfulAuthorization(copyRequestService.accept(requestId)(copyOwnerId)) {
        withNoServiceError { case (returnedRequest, returnedCopy) =>
          assert(returnedRequest.status == initialRequestStatus)
          assert(returnedCopy.status == initialCopyStatus)
        }
      }

      assert(requestIsNotUpdated(requestId, initialRequestStatus))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "not update a fulfilled request" in new WithRequestFulfilled {
      withSuccessfulAuthorization(copyRequestService.accept(requestId)(copyOwnerId)) {
        withNoServiceError { case (returnedRequest, returnedCopy) =>
          assert(returnedRequest.status == initialRequestStatus)
          assert(returnedCopy.status == initialCopyStatus)
        }
      }

      assert(requestIsNotUpdated(requestId, initialRequestStatus))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }
  }

  "The 'reject' method" should {
    "deny any request from a user other than the copy owner" in new WithRequestPending {
      withFailedAuthorization(copyRequestService.reject(requestId)(requestIssuerId)) { error =>
        error shouldBe NotTheRequestedCopyOwner(requestIssuerId, requestId)
      }
    }

    "reject a pending request" in new WithRequestPending {
      withSuccessfulAuthorization(copyRequestService.reject(requestId)(copyOwnerId)) {
        withNoServiceError { case (returnedRequest, returnedCopy) =>
          assert(returnedRequest.status.isRejected)
          assert(returnedCopy.status == initialCopyStatus)
        }
      }

      assert(requestIsRejected(requestId))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "reject a request on the waiting list" in new WithRequestOnWaitingList {
      withSuccessfulAuthorization(copyRequestService.reject(requestId)(copyOwnerId)) {
        withNoServiceError { case (returnedRequest, returnedCopy) =>
          assert(returnedRequest.status.isRejected)
          assert(returnedCopy.status == initialCopyStatus)
        }
      }

      assert(requestIsRejected(requestId))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "reject an accepted request and accept the next request on the waiting list" in new WithNextRequestOnWaitingList {
      withSuccessfulAuthorization(copyRequestService.reject(requestId)(copyOwnerId)) {
        withNoServiceError { case (returnedRequest, returnedCopy) =>
          assert(returnedRequest.status.isRejected)
          assert(returnedCopy.status == initialCopyStatus)
        }
      }

      assert(requestIsRejected(requestId))
      assert(requestIsAccepted(nextRequestId))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "reject an accepted request and update the copy status back to 'Available' if there is no waiting list" in
      new WithRequestAccepted {
        withSuccessfulAuthorization(copyRequestService.reject(requestId)(copyOwnerId)) {
          withNoServiceError { case (returnedRequest, returnedCopy) =>
            assert(returnedRequest.status.isRejected)
            assert(returnedCopy.status == CopyStatus.Available)
          }
        }

        assert(requestIsRejected(requestId))
        assert(copyIsAvailable(copyId))
      }

    "not update a rejected request" in new WithRequestRejected {
      withSuccessfulAuthorization(copyRequestService.reject(requestId)(copyOwnerId)) {
        withNoServiceError { case (returnedRequest, returnedCopy) =>
          assert(returnedRequest.status == initialRequestStatus)
          assert(returnedCopy.status == initialCopyStatus)
        }
      }

      assert(requestIsNotUpdated(requestId, initialRequestStatus))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "not update a cancelled request" in new WithRequestCancelled {
      withSuccessfulAuthorization(copyRequestService.reject(requestId)(copyOwnerId)) {
        withNoServiceError { case (returnedRequest, returnedCopy) =>
          assert(returnedRequest.status == initialRequestStatus)
          assert(returnedCopy.status == initialCopyStatus)
        }
      }

      assert(requestIsNotUpdated(requestId, initialRequestStatus))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "not update a fulfilled request" in new WithRequestFulfilled {
      withSuccessfulAuthorization(copyRequestService.reject(requestId)(copyOwnerId)) {
        withNoServiceError { case (returnedRequest, returnedCopy) =>
          assert(returnedRequest.status == initialRequestStatus)
          assert(returnedCopy.status == initialCopyStatus)
        }
      }

      assert(requestIsNotUpdated(requestId, initialRequestStatus))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }
  }

  "The 'markAsFulfilled' method" should {
    "deny any request from a user other than the copy owner" in new WithRequestAccepted {
      withFailedAuthorization(copyRequestService.markAsFulfilled(requestId)(requestIssuerId)) { error =>
        error shouldBe NotTheRequestedCopyOwner(requestIssuerId, requestId)
      }
    }

    "mark an accepted request as fulfilled and reject all requests still pending or on the waiting list" in
      new WithRequestAccepted {
        withSuccessfulAuthorization(copyRequestService.markAsFulfilled(requestId)(copyOwnerId)) {
          withNoServiceError { case (returnedRequest, returnedCopy) =>
            assert(returnedRequest.status.isFulfilled)
            assert(returnedCopy.status == CopyStatus.Swapped)
          }
        }

        assert(requestIsFulfilled(requestId))
        assert(requestIsRejected(nextRequestId))
        assert(copyIsSwapped(copyId))
      }

    "not update a pending request" in new WithRequestPending {
      withSuccessfulAuthorization(copyRequestService.markAsFulfilled(requestId)(copyOwnerId)) {
        withNoServiceError { case (returnedRequest, returnedCopy) =>
          assert(returnedRequest.status == initialRequestStatus)
          assert(returnedCopy.status == initialCopyStatus)
        }
      }

      assert(requestIsNotUpdated(requestId, initialRequestStatus))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "not update a request on the waiting list" in new WithRequestOnWaitingList {
      withSuccessfulAuthorization(copyRequestService.markAsFulfilled(requestId)(copyOwnerId)) {
        withNoServiceError { case (returnedRequest, returnedCopy) =>
          assert(returnedRequest.status == initialRequestStatus)
          assert(returnedCopy.status == initialCopyStatus)
        }
      }

      assert(requestIsNotUpdated(requestId, initialRequestStatus))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "not update a rejected request" in new WithRequestRejected {
      withSuccessfulAuthorization(copyRequestService.markAsFulfilled(requestId)(copyOwnerId)) {
        withNoServiceError { case (returnedRequest, returnedCopy) =>
          assert(returnedRequest.status == initialRequestStatus)
          assert(returnedCopy.status == initialCopyStatus)
        }
      }

      assert(requestIsNotUpdated(requestId, initialRequestStatus))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "not update a cancelled request" in new WithRequestCancelled {
      withSuccessfulAuthorization(copyRequestService.markAsFulfilled(requestId)(copyOwnerId)) {
        withNoServiceError { case (returnedRequest, returnedCopy) =>
          assert(returnedRequest.status == initialRequestStatus)
          assert(returnedCopy.status == initialCopyStatus)
        }
      }

      assert(requestIsNotUpdated(requestId, initialRequestStatus))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }

    "not update a fulfilled request" in new WithRequestFulfilled {
      withSuccessfulAuthorization(copyRequestService.markAsFulfilled(requestId)(copyOwnerId)) {
        withNoServiceError { case (returnedRequest, returnedCopy) =>
          assert(returnedRequest.status == initialRequestStatus)
          assert(returnedCopy.status == initialCopyStatus)
        }
      }

      assert(requestIsNotUpdated(requestId, initialRequestStatus))
      assert(copyIsNotUpdated(copyId, initialCopyStatus))
    }
  }

  trait WithBasicSetup {
    val userRepository = new TestUserRepository
    val copiesRepository = new TestCopiesRepository
    val copyRequestsRepository = new TestCopyRequestsRepository

    implicit val zoneId: ZoneId = ZoneId.of("UTC")

    private val catsIdTransactor = new ~>[CatsId, CatsId] {
      override def apply[T](f : CatsId[T]): CatsId[T] =
        f
    }

    val copyRequestService: CopyRequestService[CatsId] =
      CopyRequestService.create(
        authorizationByActiveStatus = instances.byActiveStatus(userRepository),
        authorizationByRequestIssuer = authorization.byRequestIssuer(copyRequestsRepository),
        authorizationByCopyOwner = authorization.byCopyOwner(copyRequestsRepository, copiesRepository),
        copyRequestsRepository,
        copiesRepository,
        catsIdTransactor
      )

    val (copyOwnerId, requestIssuerId, nextRequestIssuerId) = (Id.generate[User], Id.generate[User], Id.generate[User])
    val (copyId, requestId, nextRequestId) = (Id.generate[Copy], Id.generate[CopyRequest], Id.generate[CopyRequest])
    val initialCopyStatus = CopyStatus.available
    val (initialRequestStatus, initialNextRequestStatus) = (RequestStatus.pending, RequestStatus.pending)

    userRepository.create(User(id = requestIssuerId, name = Name("RequestIssuer"), status = UserStatus.Active))

    private val copy = Copy(
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

    val nextCopyRequest = CopyRequest(
      id = nextRequestId,
      copyId = copy.id,
      requestedBy = nextRequestIssuerId,
      requestedOn = LocalDateTime.of(2019, 9, 30, 11, 0, 0),
      status = initialNextRequestStatus
    )

    copiesRepository.create(copy)

    def copyIsAvailable(id: Id[Copy]): Boolean =
      copiesRepository.get(id).exists(_.status == CopyStatus.Available)

    def copyIsReserved(id: Id[Copy]): Boolean =
      copiesRepository.get(id).exists(_.status == CopyStatus.Reserved)

    def copyIsSwapped(id: Id[Copy]): Boolean =
      copiesRepository.get(id).exists(_.status == CopyStatus.Swapped)

    def copyIsNotUpdated(id: Id[Copy], initialCopyStatus: CopyStatus): Boolean =
      copiesRepository.get(id).exists(_.status == initialCopyStatus)

    def requestIsAccepted(id: Id[CopyRequest]): Boolean =
      copyRequestsRepository.get(id).exists(_.status.isAccepted)

    def requestIsOnWaitingList(id: Id[CopyRequest]): Boolean =
      copyRequestsRepository.get(id).exists(_.status.isOnWaitingList)

    def requestIsRejected(id: Id[CopyRequest]): Boolean =
      copyRequestsRepository.get(id).exists(_.status.isRejected)

    def requestIsFulfilled(id: Id[CopyRequest]): Boolean =
      copyRequestsRepository.get(id).exists(_.status.isFulfilled)

    def requestIsCancelled(id: Id[CopyRequest]): Boolean =
      copyRequestsRepository.get(id).exists(_.status.isCancelled)

    def requestIsNotUpdated(id: Id[CopyRequest], initialRequestStatus: RequestStatus): Boolean =
      copyRequestsRepository.get(id).exists(_.status == initialRequestStatus)
  }

  trait WithRequestPending extends WithBasicSetup {
    copyRequestsRepository.create(copyRequest)
    copyRequestsRepository.create(nextCopyRequest)
  }

  trait WithRequestAccepted extends WithRequestPending {
    override val initialRequestStatus = RequestStatus.accepted(now)
    override val initialCopyStatus = CopyStatus.reserved
    copyRequestsRepository.updateStatus(requestId, initialRequestStatus)
    copiesRepository.updateStatus(copyId, initialCopyStatus)
  }

  trait WithNextRequestOnWaitingList extends WithRequestAccepted {
    override val initialNextRequestStatus = RequestStatus.onWaitingList(now)
    copyRequestsRepository.updateStatus(nextRequestId, initialNextRequestStatus)
  }

  trait WithRequestOnWaitingList extends WithRequestPending {
    override val initialRequestStatus = RequestStatus.onWaitingList(now)
    override val initialCopyStatus = CopyStatus.reserved
    copyRequestsRepository.updateStatus(requestId, initialRequestStatus)
    copiesRepository.updateStatus(copyId, initialCopyStatus)
  }

  trait WithRequestRejected extends WithRequestPending {
    override val initialRequestStatus = RequestStatus.rejected(now)
    copyRequestsRepository.updateStatus(requestId, initialRequestStatus)
  }

  trait WithRequestCancelled extends WithRequestPending {
    override val initialRequestStatus = RequestStatus.cancelled(now)
    copyRequestsRepository.updateStatus(requestId, initialRequestStatus)
  }

  trait WithRequestFulfilled extends WithRequestPending {
    override val initialRequestStatus = RequestStatus.fulfilled(now)
    override val initialCopyStatus = CopyStatus.Swapped
    copyRequestsRepository.updateStatus(requestId, initialRequestStatus)
    copiesRepository.updateStatus(copyId, initialCopyStatus)
  }

}

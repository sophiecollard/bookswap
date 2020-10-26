package com.github.sophiecollard.bookswap.services.user

import java.time.ZoneId

import cats.{~>, Id => CatsId}
import com.github.sophiecollard.bookswap.authorization
import com.github.sophiecollard.bookswap.authorization.error.AuthorizationError.NotAnAdmin
import com.github.sophiecollard.bookswap.domain.shared.{Id, Name}
import com.github.sophiecollard.bookswap.domain.user.{User, UserStatus}
import com.github.sophiecollard.bookswap.fixtures.repositories.user.TestUsersRepository
import com.github.sophiecollard.bookswap.services.error.ServiceError.{ResourceNotFound, UserNameAlreadyTaken}
import com.github.sophiecollard.bookswap.specsyntax._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class UsersServiceSpec extends AnyWordSpec with Matchers {

  "The 'get' method" should {
    "return a user" in new WithUser {
      withRight(usersService.get(userId)) {
        _ shouldBe user
      }
    }

    "return an error if no user if found" in new WithUser {
      withLeft(usersService.get(otherUserId)) {
        _ shouldBe ResourceNotFound("User", otherUserId)
      }
    }
  }

  "The 'create' method" should {
    "create a new user" in new WithBasicSetup {
      withRight(usersService.create(user.name)) { returnedUser =>
        assert(returnedUser.name == user.name)
        assert(returnedUser.status == UserStatus.PendingVerification)

        withSome(usersRepository.get(returnedUser.id)) { createdUser =>
          assert(createdUser.name == user.name)
          assert(createdUser.status == UserStatus.PendingVerification)
        }
      }
    }

    "return an error if the user name is already taken" in new WithUser {
      withLeft(usersService.create(user.name)) {
        _ shouldBe UserNameAlreadyTaken(user.name)
      }
    }
  }

  "The 'updateStatus' method" should {
    "deny any request from a user that is not an admin" in new WithUser {
      withFailedAuthorization(usersService.updateStatus(userId, UserStatus.Admin)(userId)) {
        _ shouldBe NotAnAdmin(userId)
      }
    }

    "update a user's status" in new WithUser {
      private val updatedStatus = UserStatus.Banned

      withSuccessfulAuthorization(usersService.updateStatus(userId, updatedStatus)(adminUserId)) {
        withNoServiceError { _ =>
          withSome(usersRepository.get(userId)) { updatedUser =>
            assert(updatedUser.status == updatedStatus)
          }
        }
      }
    }

    "return an error if the user is not found" in new WithUser {
      withSuccessfulAuthorization(usersService.updateStatus(otherUserId, UserStatus.Active)(adminUserId)) {
        withServiceError {
          _ shouldBe ResourceNotFound("User", otherUserId)
        }
      }
    }
  }

  "The 'softDelete' method" should {
    "update a user's status to 'pending_deletion'" in new WithUser {
      withRight(usersService.softDelete(userId)) { _ =>
        withSome(usersRepository.get(userId)) { updatedUser =>
          assert(updatedUser.status == UserStatus.PendingDeletion)
        }
      }
    }

    "return an error if the user is not found" in new WithUser {
      withLeft(usersService.softDelete(otherUserId)) {
        _ shouldBe ResourceNotFound("User", otherUserId)
      }
    }
  }

  "The 'hardDelete' method" should {
    "deny any request from a user that is not an admin" in new WithUser {
      withFailedAuthorization(usersService.hardDelete(userId)(userId)) {
        _ shouldBe NotAnAdmin(userId)
      }
    }

    "delete a user" in new WithUser {
      withSuccessfulAuthorization(usersService.hardDelete(userId)(adminUserId)) {
        withNoServiceError { _ =>
          withNone(usersRepository.get(userId)) {
            succeed
          }
        }
      }
    }

    "return an error if the user is not found" in new WithUser {
      withSuccessfulAuthorization(usersService.hardDelete(otherUserId)(adminUserId)) {
        withServiceError {
          _ shouldBe ResourceNotFound("User", otherUserId)
        }
      }
    }
  }

  trait WithBasicSetup {
    val usersRepository = new TestUsersRepository

    implicit val zoneId: ZoneId = ZoneId.of("UTC")

    private val catsIdTransactor = new ~>[CatsId, CatsId] {
      override def apply[T](f : CatsId[T]): CatsId[T] =
        f
    }

    val usersService: UsersService[CatsId] =
      UsersService.create(
        authorization.instances.byAdminStatus(usersRepository),
        usersRepository,
        catsIdTransactor
      )

    val (userId, adminUserId, otherUserId) = (Id.generate[User], Id.generate[User], Id.generate[User])

    val user = User(userId, Name("User"), UserStatus.Active)
    val adminUser = User(adminUserId, Name("Admin"), UserStatus.Admin)

    usersRepository.create(adminUser)
  }

  trait WithUser extends WithBasicSetup {
    usersRepository.create(user)
  }

}

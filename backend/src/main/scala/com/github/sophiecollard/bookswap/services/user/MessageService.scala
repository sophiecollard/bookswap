package com.github.sophiecollard.bookswap.services.user

import cats.~>
import com.github.sophiecollard.bookswap.authorization.AuthorizationService
import com.github.sophiecollard.bookswap.authorization.instances.{ByActiveStatus, WithAuthorizationByActiveStatus}
import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.{MessagePagination, User, UserMessage}
import com.github.sophiecollard.bookswap.repositories.user.{MessageRepository, ThreadRepository}
import com.github.sophiecollard.bookswap.services.error.ServiceErrorOr
import com.github.sophiecollard.bookswap.services.user.authorization.{AuthorizationInput, ByThreadParticipant, WithAuthorizationByThreadParticipant}

trait MessageService[F[_]] {

  // TODO The model shouldn't allow for opening more than one thread per pair of participants
  def startThread(
    participant: Id[User],
    openingMessageContents: String
  )(
    userId: Id[User]
  ): F[WithAuthorizationByActiveStatus[ServiceErrorOr[Id[Thread]]]]

  def createMessage(
    threadId: Id[Thread],
    contents: String
  )(
    userId: Id[User]
  ): F[WithAuthorizationByThreadParticipant[ServiceErrorOr[UserMessage]]]

  def getMessages(
    threadId: Id[Thread],
    pagination: MessagePagination
  )(
    userId: Id[User]
  ): F[WithAuthorizationByThreadParticipant[ServiceErrorOr[List[UserMessage]]]]

}

object MessageService {

  def create[F[_], G[_]](
    authorizationByActiveStatus: AuthorizationService[F, Id[User], ByActiveStatus],
    authorizationByThreadParticipant: AuthorizationService[F, AuthorizationInput, ByThreadParticipant],
    threadRepository: ThreadRepository[G],
    messageRepository: MessageRepository[G],
    transactor: G ~> F
  ): MessageService[F] = new MessageService[F] {
    override def startThread(
      participant: Id[User],
      openingMessageContents: String
    )(
      userId: Id[User]
    ): F[WithAuthorizationByActiveStatus[ServiceErrorOr[Id[Thread]]]] =
      authorizationByActiveStatus.authorize(userId) {
//        threadRepository
        ???
      }

    override def createMessage(
      threadId: Id[Thread],
      contents: String
    )(
      userId: Id[User]
    ): F[WithAuthorizationByThreadParticipant[ServiceErrorOr[UserMessage]]] =
      authorizationByThreadParticipant.authorize(AuthorizationInput(userId, threadId)) {
        ???
      }

    override def getMessages(
      threadId: Id[Thread],
      pagination: MessagePagination
    )(
      userId: Id[User]
    ): F[WithAuthorizationByThreadParticipant[ServiceErrorOr[List[UserMessage]]]] =
      authorizationByThreadParticipant.authorize(AuthorizationInput(userId, threadId)) {
        ???
      }
  }

}

package com.github.sophiecollard.bookswap.repositories.user

import com.github.sophiecollard.bookswap.domain.shared.Id
import com.github.sophiecollard.bookswap.domain.user.{MessagePagination, UserMessage}

trait MessageRepository[F[_]] {

  def create(threadId: Id[Thread], message: UserMessage): F[Boolean]

  def get(threadId: Id[Thread], pagination: MessagePagination): F[List[UserMessage]]

}

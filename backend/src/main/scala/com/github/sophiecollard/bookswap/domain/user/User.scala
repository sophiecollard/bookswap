package com.github.sophiecollard.bookswap.domain.user

import com.github.sophiecollard.bookswap.domain.shared.{Id, Name}

final case class User(id: Id[User], name: Name[User], status: UserStatus)

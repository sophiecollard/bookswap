package com.github.sophiecollard.bookswap.domain.inventory

import com.github.sophiecollard.bookswap.domain.shared.{Id, Name}

final case class Author(id: Id[Author], name: Name[Author])

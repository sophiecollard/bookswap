package com.github.sophiecollard.bookswap.domain.inventory

import com.github.sophiecollard.bookswap.domain.shared.{Id, Name}

final case class Publisher(id: Id[Publisher], name: Name[Publisher])

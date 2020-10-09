package com.github.sophiecollard.bookswap.repositories.instances

import cats.implicits._
import doobie.util.meta.Meta
import io.circe.Json
import io.circe.parser.parse
import org.postgresql.util.PGobject

trait json {

  implicit val jsonbMeta: Meta[Json] =
    Meta.Advanced.other[PGobject]("jsonb").timap[Json] { pgObject =>
      parse(pgObject.getValue).leftMap(e => throw e).merge
    } { json =>
      val pGobject = new PGobject
      pGobject.setType("jsonb")
      pGobject.setValue(json.noSpaces)
      pGobject
    }

}

object json extends json

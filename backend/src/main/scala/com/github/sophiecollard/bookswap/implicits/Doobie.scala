package com.github.sophiecollard.bookswap.implicits

import cats.data.NonEmptyList
import cats.implicits._
import doobie.util.meta.Meta
import io.circe.{Decoder, Encoder, Json}
import io.circe.parser.parse
import io.circe.syntax._
import org.postgresql.util.PGobject

trait Doobie {

  implicit val jsonMeta: Meta[Json] =
    Meta.Advanced.other[PGobject]("jsonb").timap[Json] { pgObject =>
      parse(pgObject.getValue).leftMap(e => throw e).merge
    } { json =>
      val pGobject = new PGobject
      pGobject.setType("jsonb")
      pGobject.setValue(json.noSpaces)
      pGobject
    }

  implicit def nelMeta[A](implicit e: Encoder[A], d: Decoder[A]): Meta[NonEmptyList[A]] =
    Meta[Json].imap { json =>
      json.as[NonEmptyList[A]].fold(
        _ => throw new RuntimeException(s"Failed to decode NonEmptyList[A] from: $json"),
        identity
      )
    } (_.asJson)

}

object Doobie extends Doobie

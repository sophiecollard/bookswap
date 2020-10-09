package com.github.sophiecollard.bookswap.repositories.instances

import cats.data.NonEmptyList
import doobie.util.meta.Meta
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json}

trait collection {

  implicit def nelEncoder[A](implicit ev: Encoder[A]): Encoder[NonEmptyList[A]] =
    Encoder[List[A]].contramap(_.toList)

  implicit def nelDecoder[A](implicit ev: Decoder[A]): Decoder[NonEmptyList[A]] =
    Decoder[List[A]].emap {
      case Nil =>
        Left("Failed to decode NonEmptyList from empty List")
      case head :: tail =>
        Right(NonEmptyList.of(head, tail: _*))
    }

  implicit def nelMeta[A](implicit e: Encoder[A], d: Decoder[A]): Meta[NonEmptyList[A]] =
    Meta[Json].imap { json =>
      json.as[NonEmptyList[A]].fold(
        _ => throw new RuntimeException(s"Failed to decode NonEmptyList[A] from: $json"),
        identity
      )
    } (_.asJson)

}

object collection extends collection

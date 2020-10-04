package com.github.sophiecollard.bookswap.instances

import cats.data.NonEmptyList
import io.circe.{Decoder, Encoder}

trait Circe {

  implicit def nelEncoder[A](implicit ev: Encoder[A]): Encoder[NonEmptyList[A]] =
    Encoder[List[A]].contramap(_.toList)

  implicit def nelDecoder[A](implicit ev: Decoder[A]): Decoder[NonEmptyList[A]] =
    Decoder[List[A]].emap {
      case Nil =>
        Left("Failed to decode NonEmptyList")
      case head :: tail =>
        Right(NonEmptyList.of(head, tail: _*))
    }

}

object Circe extends Circe

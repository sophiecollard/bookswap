package com.github.sophiecollard.bookswap.domain.shared

import cats.data.Validated
import cats.data.Validated.Valid
import io.circe.Decoder.{AccumulatingResult, Result}
import io.circe._
import io.circe.syntax._

sealed trait MaybeUpdate[+A] {
  final def getOrElse[AA >: A](ifNoUpdate: AA): AA = this match {
    case MaybeUpdate.Update(a) => a
    case MaybeUpdate.NoUpdate  => ifNoUpdate
  }
}

object MaybeUpdate {

  final case class Update[A](value: A) extends MaybeUpdate[A]
  case object NoUpdate extends MaybeUpdate[Nothing]

  def update[A](value: A): MaybeUpdate[A] = Update(value)
  def noUpdate[A]: MaybeUpdate[A] = NoUpdate

  implicit def encoder[A](implicit ev: Encoder[A]): Encoder[MaybeUpdate[A]] =
    Encoder.instance {
      case Update(value) => Json.obj("update" := true, "value" := value)
      case NoUpdate => Json.obj("update" := false)
    }

  implicit def decoder[A](implicit ev: Decoder[A]): Decoder[MaybeUpdate[A]] = new Decoder[MaybeUpdate[A]] {
    override def apply(c: HCursor): Result[MaybeUpdate[A]] = tryDecode(c)

    final override def tryDecode(c: ACursor): Decoder.Result[MaybeUpdate[A]] = c match {
      case c: HCursor =>
        if (c.value.isNull) Right(noUpdate[A])
        else strictDecoder[A].tryDecode(c)
      case c: FailedCursor =>
        if (!c.incorrectFocus) Right(noUpdate[A])
        else Left(DecodingFailure("[A]MaybeUpdate[A]", c.history))
    }

    final override def decodeAccumulating(c: HCursor): AccumulatingResult[MaybeUpdate[A]] = tryDecodeAccumulating(c)

    final override def tryDecodeAccumulating(c: ACursor): AccumulatingResult[MaybeUpdate[A]] = c match {
      case c: HCursor =>
        if (c.value.isNull) Valid(noUpdate[A])
        else strictDecoder[A].tryDecodeAccumulating(c)
      case c: FailedCursor =>
        if (!c.incorrectFocus) Valid(noUpdate[A])
        else Validated.invalidNel(DecodingFailure("[A]MaybeUpdate[A]", c.history))
    }
  }

  private def strictDecoder[A](implicit ev: Decoder[A]): Decoder[MaybeUpdate[A]] =
    Decoder.instance { cursor =>
      cursor.downField("update").as[Boolean].flatMap { doUpdate =>
        if (doUpdate) cursor.downField("value").as[A].map(update[A])
        else Right(noUpdate[A])
      }
    }

}

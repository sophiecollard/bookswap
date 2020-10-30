package com.github.sophiecollard.bookswap.domain.user

import doobie.Meta
import io.circe.{Decoder, Encoder}

sealed abstract case class Email(value: String)

object Email {

  def apply(value: String): Option[Email] = {
    val emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,12}$".r
    emailPattern findFirstIn value map {
      new Email(_) {}
    }
  }

  def unsafeApply(value: String): Email =
    apply(value).get

  implicit val meta: Meta[Email] =
    Meta[String].imap(unsafeApply)(_.value)

  implicit val encoder: Encoder[Email] =
    Encoder[String].contramap(_.value)

  implicit val decoder: Decoder[Email] =
    Decoder[String].emap { string =>
      apply(string) match {
        case Some(email) => Right(email)
        case None        => Left(s"Failed to parse Email from $string")
      }
    }

}

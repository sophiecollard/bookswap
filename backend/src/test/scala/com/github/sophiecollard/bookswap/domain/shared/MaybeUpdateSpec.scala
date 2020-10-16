package com.github.sophiecollard.bookswap.domain.shared

import com.github.sophiecollard.bookswap.domain.shared.MaybeUpdate.{NoUpdate, Update}
import io.circe.generic.semiauto
import io.circe.syntax._
import io.circe.{Decoder, Encoder, Json, parser}
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MaybeUpdateSpec extends AnyWordSpec with Matchers {

  "The Encoder instance" should {

    "encode Update instances" in {
      val userUpdate = UserUpdate(
        name = Update("Gargamel"),
        age = Update(None),
        occupation = Update(Some("Wizzard"))
      )

      val expectedResult = Json.obj(
        "name" := Json.obj("update" := true, "value" := "Gargamel"),
        "age" := Json.obj("update" := true, "value" := Json.Null),
        "occupation" := Json.obj("update" := true, "value" := "Wizzard")
      )

      Encoder[UserUpdate].apply(userUpdate) shouldBe expectedResult
    }

    "encode NoUpdate instances" in {
      val userUpdate = UserUpdate(
        name = NoUpdate,
        age = NoUpdate,
        occupation = NoUpdate
      )

      val expectedResult = Json.obj(
        "name" := Json.obj("update" := false),
        "age" := Json.obj("update" := false),
        "occupation" := Json.obj("update" := false)
      )

      Encoder[UserUpdate].apply(userUpdate) shouldBe expectedResult
    }

  }

  "The Decoder instance" should {

    "decode Update instances" in {
      val rawJson =
        s"""
           |{
           |  "name": {
           |    "update": true,
           |    "value": "Gargamel"
           |  },
           |  "age": {
           |    "update": true,
           |    "value": null
           |  },
           |  "occupation": {
           |    "update": true,
           |    "value": "Wizzard"
           |  }
           |}
           |""".stripMargin

      val expectedResult = UserUpdate(
        name = Update("Gargamel"),
        age = Update(None),
        occupation = Update(Some("Wizzard"))
      )

      withParsedJson(rawJson) { json =>
        withDecodedEntity[UserUpdate](json) { userUpdate =>
          userUpdate shouldBe expectedResult
        }

        withAccDecodedEntity[UserUpdate](json) { userUpdate =>
          userUpdate shouldBe expectedResult
        }
      }
    }

    "decode NoUpdate instances" in {
      val rawJson =
        s"""
           |{
           |  "name": {
           |    "update": false
           |  },
           |  "age": {
           |    "update": false
           |  },
           |  "occupation": {
           |    "update": false
           |  }
           |}
           |""".stripMargin

      val expectedResult = UserUpdate(
        name = NoUpdate,
        age = NoUpdate,
        occupation = NoUpdate
      )

      withParsedJson(rawJson) { json =>
        withDecodedEntity[UserUpdate](json) { userUpdate =>
          userUpdate shouldBe expectedResult
        }

        withAccDecodedEntity[UserUpdate](json) { userUpdate =>
          userUpdate shouldBe expectedResult
        }
      }
    }

    "decode null values as NoUpdate instances" in {
      val rawJson =
        s"""
           |{
           |  "name": null,
           |  "age": null,
           |  "occupation": null
           |}
           |""".stripMargin

      val expectedResult = UserUpdate(
        name = NoUpdate,
        age = NoUpdate,
        occupation = NoUpdate
      )

      withParsedJson(rawJson) { json =>
        withDecodedEntity[UserUpdate](json) { userUpdate =>
          userUpdate shouldBe expectedResult
        }

        withAccDecodedEntity[UserUpdate](json) { userUpdate =>
          userUpdate shouldBe expectedResult
        }
      }
    }

    "decode missing fields as NoUpdate instances" in {
      val rawJson =
        s"""
           |{}
           |""".stripMargin

      val expectedResult = UserUpdate(
        name = NoUpdate,
        age = NoUpdate,
        occupation = NoUpdate
      )

      withParsedJson(rawJson) { json =>
        withDecodedEntity[UserUpdate](json) { userUpdate =>
          userUpdate shouldBe expectedResult
        }

        withAccDecodedEntity[UserUpdate](json) { userUpdate =>
          userUpdate shouldBe expectedResult
        }
      }
    }

  }

  private def withParsedJson(
    rawJson: String
  )(
    ifSuccess: Json => Assertion
  ): Assertion = {
    val result = parser.parse(rawJson)
    assert(result.isRight)
    ifSuccess(result.toOption.get)
  }

  private def withDecodedEntity[A](
    json: Json
  )(
    ifSuccess: A => Assertion
  )(
    implicit decoder: Decoder[A]
  ): Assertion = {
    val result = decoder.decodeJson(json)
    assert(result.isRight)
    ifSuccess(result.toOption.get)
  }

  private def withAccDecodedEntity[A](
    json: Json
  )(
    ifSuccess: A => Assertion
  )(
    implicit decoder: Decoder[A]
  ): Assertion = {
    val result = decoder.decodeAccumulating(json.hcursor)
    assert(result.isValid)
    ifSuccess(result.toOption.get)
  }

}

final case class UserUpdate(
  name: MaybeUpdate[String],
  age: MaybeUpdate[Option[Int]],
  occupation: MaybeUpdate[Option[String]]
)

object UserUpdate {
  implicit val encoder: Encoder[UserUpdate] = semiauto.deriveEncoder
  implicit val decoder: Decoder[UserUpdate] = semiauto.deriveDecoder
}

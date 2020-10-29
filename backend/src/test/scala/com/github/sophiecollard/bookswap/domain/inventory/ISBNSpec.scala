package com.github.sophiecollard.bookswap.domain.inventory

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ISBNSpec extends AnyWordSpec with Matchers {

  "The 'apply' method" should {

    "parse a 13-digit ISBN" in {
      val maybeISBN = ISBN("9781784875435")
      assert(maybeISBN.isDefined)
      assert(maybeISBN.get.value == "9781784875435")
    }

    "parse a 10-digit ISBN" in {
      val maybeISBN = ISBN("8175257660")
      assert(maybeISBN.isDefined)
      assert(maybeISBN.get.value == "8175257660")
    }

    "strip dashes and spaces before attempting to parse an ISBN" in {
      val maybe13DigitISBN = ISBN("978 2290155158")
      assert(maybe13DigitISBN.isDefined)
      assert(maybe13DigitISBN.get.value == "9782290155158")
      val maybe10DigitISBN = ISBN("2-226-05257-7")
      assert(maybe10DigitISBN.isDefined)
      assert(maybe10DigitISBN.get.value == "2226052577")
    }

    "fail to parse any other input" in {
      assert(ISBN("ABC1234567").isEmpty) // contains non-digit characters
      assert(ISBN("9771784875435").isEmpty) // does not start with '978' or '979'
      assert(ISBN("978178487543").isEmpty) // 12-digit
      assert(ISBN("97817848754356").isEmpty) // 14-digit
    }

  }

  "The 'language' method" should {

    "return the language a book was most probably published in" in {
      ISBN("0-330-28498-3").get.language shouldBe Some(Language.English)
      ISBN("1-58182-008-9").get.language shouldBe Some(Language.English)
      ISBN("978-2290155158").get.language shouldBe Some(Language.French)
      ISBN("3-7965-1900-8").get.language shouldBe Some(Language.German)
      ISBN("4-19-830127-1").get.language shouldBe Some(Language.Japanese)
      ISBN("5-85270-001-0").get.language shouldBe Some(Language.Russian)
    }

  }

}

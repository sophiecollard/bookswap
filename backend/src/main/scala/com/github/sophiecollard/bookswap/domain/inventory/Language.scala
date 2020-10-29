package com.github.sophiecollard.bookswap.domain.inventory

sealed abstract class Language(val name: String, val iso6391Code: String)

object Language {

  case object Bulgarian  extends Language("Bulgarian", "BG")
  case object Chinese    extends Language("Chinese", "ZH")
  case object Croatian   extends Language("Croatian", "HR")
  case object Czech      extends Language("Czech", "CS")
  case object Danish     extends Language("Danish", "DA")
  case object Dutch      extends Language("Dutch", "NL")
  case object English    extends Language("English", "EN")
  case object Estonian   extends Language("Estonian", "ET")
  case object Finnish    extends Language("Finnish", "FI")
  case object French     extends Language("French", "FR")
  case object German     extends Language("German", "DE")
  case object Greek      extends Language("Greek", "EL")
  case object Hungarian  extends Language("Hungarian", "HU")
  case object Irish      extends Language("Irish", "GA")
  case object Italian    extends Language("Italian", "IT")
  case object Japanese   extends Language("Japanese", "JA")
  case object Korean     extends Language("Korean", "KO")
  case object Latvian    extends Language("Latvian", "LV")
  case object Lithuanian extends Language("Lithuanian", "LT")
  case object Maltese    extends Language("Maltese", "MT")
  case object Norwegian  extends Language("Norwegian", "NO")
  case object Polish     extends Language("Polish", "PL")
  case object Portuguese extends Language("Portuguese", "PT")
  case object Romanian   extends Language("Romanian", "RO")
  case object Russian    extends Language("Russian", "RU")
  case object Slovak     extends Language("Slovak", "SK")
  case object Slovenian  extends Language("Slovenian", "SL")
  case object Spanish    extends Language("Spanish", "ES")
  case object Swedish    extends Language("Swedish", "SV")

}

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common

import java.time.OffsetDateTime

fun String.isEmail() = this.matches(
  Regex("[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}\\@[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+"),
)

fun String.toOffsetDateTime(): OffsetDateTime = OffsetDateTime.parse(this)

fun String.capitalisedWords() = lowercase().split("\\s+".toRegex()).joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }

fun String.allCapitals(matching: String) = replace(matching, matching, true)

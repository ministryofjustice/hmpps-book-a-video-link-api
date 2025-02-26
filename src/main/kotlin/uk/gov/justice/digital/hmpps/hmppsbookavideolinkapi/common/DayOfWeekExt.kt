package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common

import java.time.DayOfWeek

fun DayOfWeek.between(from: Int, to: Int) = run {
  require(from >= 1 || from <= 7) {
    "Invalid value for 'from' DayOfWeek: $from"
  }

  require(to >= 1 || to <= 7) {
    "Invalid value for 'to' DayOfWeek: $to"
  }

  this.value in from..to
}

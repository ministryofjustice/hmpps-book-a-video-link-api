package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common

import java.time.LocalTime

fun isTimesOverlap(startOne: LocalTime, endOne: LocalTime, startTwo: LocalTime, endTwo: LocalTime) = !(endOne.isOnOrBefore(startTwo) || startOne.isOnOrAfter(endTwo))

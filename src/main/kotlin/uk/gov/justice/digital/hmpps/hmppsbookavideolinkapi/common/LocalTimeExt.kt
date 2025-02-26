package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

fun LocalTime.isBetween(from: LocalTime, to: LocalTime) = this.isOnOrAfter(from) && this.isOnOrBefore(to)

fun LocalTime.toMinutePrecision(): LocalTime = this.truncatedTo(ChronoUnit.MINUTES)

fun LocalTime.isOnOrBefore(other: LocalTime) = this <= other

fun LocalTime.isOnOrAfter(other: LocalTime) = this >= other

fun LocalTime.toHourMinuteStyle(): String = this.format(DateTimeFormatter.ofPattern("HH:mm"))

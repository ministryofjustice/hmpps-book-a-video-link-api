package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common

import java.time.LocalTime
import java.time.temporal.ChronoUnit

fun LocalTime.toMinutePrecision() = this.truncatedTo(ChronoUnit.MINUTES)

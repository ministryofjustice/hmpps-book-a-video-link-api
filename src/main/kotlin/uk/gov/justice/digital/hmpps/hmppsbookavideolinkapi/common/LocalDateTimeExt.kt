package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun LocalDateTime.toIsoDateTime(): String = this.format(DateTimeFormatter.ISO_DATE_TIME)

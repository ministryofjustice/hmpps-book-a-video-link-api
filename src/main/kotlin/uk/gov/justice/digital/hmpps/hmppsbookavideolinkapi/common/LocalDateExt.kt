package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun LocalDate.toMediumFormatStyle(): String = this.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))

fun LocalDate.toIsoDate(): String = this.format(DateTimeFormatter.ISO_DATE)

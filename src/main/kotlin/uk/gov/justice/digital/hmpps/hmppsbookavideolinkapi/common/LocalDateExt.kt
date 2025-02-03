package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun LocalDate.toMediumFormatStyle(): String = this.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))

fun LocalDate.toIsoDate(): String = this.format(DateTimeFormatter.ISO_DATE)

fun LocalDateTime.toOffsetString(): String = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(this.atOffset(ZoneId.of("Europe/London").rules.getOffset(this)))

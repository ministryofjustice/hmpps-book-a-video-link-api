package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper

import java.time.LocalDate

fun today(): LocalDate = LocalDate.now()

fun yesterday(): LocalDate = today().minusDays(1)
fun tomorrow(): LocalDate = today().plusDays(1)

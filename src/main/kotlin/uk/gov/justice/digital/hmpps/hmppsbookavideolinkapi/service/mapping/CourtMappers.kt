package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Court as CourtEntity

fun CourtEntity.toModel() = Court(
  courtId = courtId,
  code = code,
  description = description,
  enabled = enabled,
  notes = notes,
)

fun List<CourtEntity>.toModel() = map { it.toModel() }.sortedBy { it.description }

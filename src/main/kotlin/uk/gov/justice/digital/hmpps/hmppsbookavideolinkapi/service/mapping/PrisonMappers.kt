package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Prison as PrisonEntity

fun PrisonEntity.toModel() = Prison(
  prisonId = prisonId,
  code = code,
  name = name,
  enabled = enabled,
  notes = notes,
)

fun List<PrisonEntity>.toModel() = map { it.toModel() }.sortedBy { it.name }

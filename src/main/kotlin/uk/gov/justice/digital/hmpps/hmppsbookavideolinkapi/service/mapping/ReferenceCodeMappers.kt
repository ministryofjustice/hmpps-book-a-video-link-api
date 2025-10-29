package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ReferenceCode as ReferenceCodeEntity

fun ReferenceCodeEntity.toModel() = ReferenceCode(
  referenceCodeId = referenceCodeId,
  groupCode = groupCode,
  code = code,
  description = description,
  enabled = enabled,
  displaySequence = displaySequence,
)

fun List<ReferenceCodeEntity>.toModel() = map { it.toModel() }

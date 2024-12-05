package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.capitalisedWords
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location as ModelLocation

fun Location.toModel() = ModelLocation(
  key = key,
  description = localName?.capitalisedWords(),
  enabled = active,
)

fun List<Location>.toModel() = map { it.toModel() }.sortedBy { it.description }

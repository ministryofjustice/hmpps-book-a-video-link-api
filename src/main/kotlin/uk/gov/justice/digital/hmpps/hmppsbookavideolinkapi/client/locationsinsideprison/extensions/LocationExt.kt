package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.extensions

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location

fun Location.isActive() = status == Location.Status.ACTIVE

fun Location.isAtPrison(prisonCode: String) = prisonId == prisonCode

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prisoner as ModelPrisoner

fun Prisoner.toPrisonerDetails() = ModelPrisoner(
  prisonerNumber = this.prisonerNumber,
  prisonCode = this.prisonId!!,
  firstName = this.firstName,
  lastName = this.lastName,
  dateOfBirth = this.dateOfBirth,
)

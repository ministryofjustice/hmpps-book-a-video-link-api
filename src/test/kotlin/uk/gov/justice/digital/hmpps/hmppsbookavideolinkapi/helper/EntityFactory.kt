package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ProbationTeam

fun court(courtId: Long = 0) = Court(
  courtId = courtId,
  code = "code",
  description = "description",
  enabled = true,
  notes = null,
  createdBy = "Test",
)

fun probationTeam(probationTeamId: Long = 0) = ProbationTeam(
  probationTeamId = probationTeamId,
  code = "code",
  description = "description",
  enabled = true,
  notes = null,
  createdBy = "Test",
)

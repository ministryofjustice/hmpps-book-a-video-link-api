package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ProbationTeam

fun court(courtId: Long = 0, enabled: Boolean = true) = Court(
  courtId = courtId,
  code = "code",
  description = "description",
  enabled = enabled,
  notes = null,
  createdBy = "Test",
)

fun probationTeam(probationTeamId: Long = 0, enabled: Boolean = true) = ProbationTeam(
  probationTeamId = probationTeamId,
  code = "code",
  description = "description",
  enabled = enabled,
  notes = null,
  createdBy = "Test",
)

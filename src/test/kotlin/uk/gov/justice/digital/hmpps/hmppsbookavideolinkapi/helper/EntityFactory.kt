package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ProbationTeam

fun court(courtId: Long = 0, enabled: Boolean = true) = Court(
  courtId = courtId,
  code = "code",
  description = "court description",
  enabled = enabled,
  notes = null,
  createdBy = "Test",
)

fun prison(prisonCode: String, enabled: Boolean = true) = Prison(
  prisonId = 0,
  code = prisonCode,
  description = "prison description",
  enabled = enabled,
  notes = null,
  createdBy = "Test",
)

fun probationTeam(probationTeamId: Long = 0, enabled: Boolean = true) = ProbationTeam(
  probationTeamId = probationTeamId,
  code = "code",
  description = "probation team description",
  enabled = enabled,
  notes = null,
  createdBy = "Test",
)

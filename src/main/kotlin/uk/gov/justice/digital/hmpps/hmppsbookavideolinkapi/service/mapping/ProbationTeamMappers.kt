package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.ProbationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ProbationTeam as ProbationTeamEntity

fun ProbationTeamEntity.toModel() = ProbationTeam(
  probationTeamId = probationTeamId,
  code = code,
  description = description,
  enabled = enabled,
  notes = notes,
  courtTeam = courtTeam,
  sentenceManagementTeam = sentenceManagementTeam,
)

fun List<ProbationTeamEntity>.toModel() = map { it.toModel() }

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.toModel
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository

@Service
class ProbationTeamsService(
  private val probationTeamRepository: ProbationTeamRepository,
) {

  fun getEnabledProbationTeams() =
    probationTeamRepository.findAllByEnabledIsTrue().toModel()
}

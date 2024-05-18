package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.UserProbation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.SetProbationTeamPreferencesRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.SetProbationTeamPreferencesResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.UserProbationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel

@Service
class ProbationTeamsService(
  private val probationTeamRepository: ProbationTeamRepository,
  private val userProbationRepository: UserProbationRepository,
) {
  fun getEnabledProbationTeams() =
    probationTeamRepository.findAllByEnabledIsTrue().toModel()

  fun getUserProbationTeamPreferences(username: String) =
    probationTeamRepository.findProbationTeamsByUsername(username).toModel()

  @Transactional
  fun setUserProbationTeamPreferences(
    request: SetProbationTeamPreferencesRequest,
    username: String,
  ): SetProbationTeamPreferencesResponse {
    userProbationRepository.findAllByUsername(username).forEach(userProbationRepository::delete)

    val newTeams = probationTeamRepository.findAllByCodeIn(request.probationTeamCodes).filter { it.enabled }

    // Recreate the UserCourt entities for these courts including the primary key of the related court
    newTeams.map { probationTeam ->
      UserProbation(probationTeam = probationTeam, username = username, createdBy = username)
    }.forEach(userProbationRepository::saveAndFlush)

    return SetProbationTeamPreferencesResponse(probationTeamsSaved = newTeams.size)
  }
}

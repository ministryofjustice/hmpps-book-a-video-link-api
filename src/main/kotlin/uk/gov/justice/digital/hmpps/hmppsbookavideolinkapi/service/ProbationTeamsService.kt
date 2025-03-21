package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.CacheConfiguration
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ProbationTeam
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
  @Cacheable(CacheConfiguration.PROBATION_TEAMS_CACHE)
  fun getProbationTeams(enabledOnly: Boolean) = if (enabledOnly) {
    probationTeamRepository.findAllByEnabledIsTrue()
  } else {
    probationTeamRepository.findAll().filter(ProbationTeam::isReadable)
  }.orderedByEnabledAndDescription().toModel()

  /**
   * This means disabled teams will show ordered by description at the bottom of the list.
   */
  private fun List<ProbationTeam>.orderedByEnabledAndDescription() = sortedWith(compareBy({ !it.enabled }, { it.description }))

  fun getUserProbationTeamPreferences(user: User) = probationTeamRepository.findProbationTeamsByUsername(user.username).toModel()

  @Transactional
  fun setUserProbationTeamPreferences(request: SetProbationTeamPreferencesRequest, user: User): SetProbationTeamPreferencesResponse {
    userProbationRepository.findAllByUsername(user.username).forEach(userProbationRepository::delete)

    val newTeams = probationTeamRepository.findAllByCodeIn(request.probationTeamCodes).filter { it.enabled }
    newTeams.map { probationTeam ->
      UserProbation(probationTeam = probationTeam, username = user.username, createdBy = user.username)
    }.forEach(userProbationRepository::saveAndFlush)

    return SetProbationTeamPreferencesResponse(probationTeamsSaved = newTeams.size)
  }
}

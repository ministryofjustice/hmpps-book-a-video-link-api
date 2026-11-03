package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.CacheConfiguration
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ProbationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.UserProbation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.SetProbationTeamPreferencesRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.MigratedProbationTeamPreferencesResponse
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

  @Transactional
  fun migrateProbationTeamPreferences(user: DeliusUser): MigratedProbationTeamPreferencesResponse {
    val currentTeams = userProbationRepository.findAllByUsername(user.username)
    if (currentTeams.isNotEmpty() || user.email.isNullOrEmpty()) {
      return MigratedProbationTeamPreferencesResponse(probationTeamsMigrated = 0)
    }

    // Look for the equivalent external account preferences for the email address of this user
    val teamsCodesForExternalAccount = probationTeamRepository.findProbationTeamsByUsername(user.email)
      .map { it.code }

    // Make sure only to take probation teams that are enabled
    val newTeams = probationTeamRepository.findAllByCodeIn(teamsCodesForExternalAccount)
      .filter { it.enabled }

    // Recreate these preferences for the current user
    newTeams.map { probationTeam ->
      UserProbation(probationTeam = probationTeam, username = user.username, createdBy = user.username)
    }.forEach(userProbationRepository::saveAndFlush)

    return MigratedProbationTeamPreferencesResponse(probationTeamsMigrated = newTeams.size)
  }
}

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

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
    log.info("Migrate delius preferences: Request to migrate teams to ${user.username} with email address ${user.email}")

    // Double check if they have any current team preferences and if so, silently ignore
    val currentTeams = userProbationRepository.findAllByUsername(user.username)
    if (currentTeams.isNotEmpty() || user.email.isNullOrEmpty()) {
      log.info("Migrate delius preferences: No migration as teams is not empty (size is ${currentTeams.size}) or user email is empty ${user.email}")
      return MigratedProbationTeamPreferencesResponse(probationTeamsMigrated = 0)
    }

    log.info("Migrate delius preferences: No current preferences set for nDelius user ${user.username}")

    // Look for the equivalent external account preferences for the email address (must be uppercase) of the user
    // If present, it means they previously had an external account, where their email address is their username.
    val teamsCodesForExternalAccount = probationTeamRepository.findProbationTeamsByUsername(user.email.uppercase())
      .map { it.code }

    log.info("Migrate delius preferences: Checking external account for ${user.email.uppercase()} found ${teamsCodesForExternalAccount.size} teams")

    // Make sure only to take probation teams that are enabled
    val newTeams = probationTeamRepository.findAllByCodeIn(teamsCodesForExternalAccount)
      .filter { it.enabled }

    log.info("Migrate delius preferences: Filter to enabled teams leaves ${newTeams.size} teams to migrate from ${user.email.uppercase()}")

    // Recreate these preferences for the current user
    newTeams.map { probationTeam ->
      UserProbation(probationTeam = probationTeam, username = user.username, createdBy = user.username)
    }.forEach(userProbationRepository::saveAndFlush)

    log.info("Migrate delius preferences: Migrated ${newTeams.size} teams from ${user.email.uppercase()} to ${user.username}")

    return MigratedProbationTeamPreferencesResponse(probationTeamsMigrated = newTeams.size)
  }
}

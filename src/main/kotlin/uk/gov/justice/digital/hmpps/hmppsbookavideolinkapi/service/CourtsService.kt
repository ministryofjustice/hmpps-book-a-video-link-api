package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.BvlsRequestContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.UserCourt
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.SetCourtPreferencesRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.SetCourtPreferencesResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.UserCourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel

@Service
class CourtsService(
  private val courtRepository: CourtRepository,
  private val userCourtRepository: UserCourtRepository,
) {
  fun getEnabledCourts() =
    courtRepository.findAllByEnabledIsTrue().toModel()

  fun getUserCourtPreferences(username: String) =
    courtRepository.findCourtsByUsername(username).toModel()

  @Transactional
  fun setUserCourtPreferences(
    request: SetCourtPreferencesRequest,
    context: BvlsRequestContext,
  ): SetCourtPreferencesResponse {
    // Find and remove the existing court preferences for this user and remove them
    val existingCourtPreferences = userCourtRepository.findAllByUsername(context.username)
    existingCourtPreferences?.forEach(userCourtRepository::delete)

    // Get the court entities for the codes in the request and filter any not enabled
    val newCourts = courtRepository.findAllByCodeIn(request.courtCodes).filter { it.enabled }

    // Recreate the UserCourt entities for these courts including the primary key of the related court
    newCourts.map { court ->
      UserCourt(
        court = court,
        username = context.username,
        createdBy = context.username,
      )
    }.forEach(userCourtRepository::saveAndFlush)

    // Return the number of court preferences saved
    return SetCourtPreferencesResponse(courtsSaved = newCourts.size)
  }
}

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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

  fun getUserCourtPreferences(user: User) =
    courtRepository.findCourtsByUsername(user.username).toModel()

  @Transactional
  fun setUserCourtPreferences(request: SetCourtPreferencesRequest, user: User): SetCourtPreferencesResponse {
    userCourtRepository.findAllByUsername(user.username).forEach(userCourtRepository::delete)

    val newCourts = courtRepository.findAllByCodeIn(request.courtCodes).filter { it.enabled }
    newCourts.map { court ->
      UserCourt(court = court, username = user.username, createdBy = user.username)
    }.forEach(userCourtRepository::saveAndFlush)

    return SetCourtPreferencesResponse(courtsSaved = newCourts.size)
  }
}

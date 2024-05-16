package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel

@Service
class CourtsService(private val courtRepository: CourtRepository) {
  fun getEnabledCourts() =
    courtRepository.findAllByEnabledIsTrue().toModel()

  fun getUserCourtPreferences(username: String) =
    courtRepository.findCourtsByUsername(username).toModel()
}

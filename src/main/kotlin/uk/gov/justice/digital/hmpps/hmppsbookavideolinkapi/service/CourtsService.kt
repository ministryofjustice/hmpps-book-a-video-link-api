package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.toModel
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository

@Service
class CourtsService(
  private val courtRepository: CourtRepository,
) {

  fun getEnabledCourts()
    = courtRepository.findAllByEnabledIsTrue().toModel()
}

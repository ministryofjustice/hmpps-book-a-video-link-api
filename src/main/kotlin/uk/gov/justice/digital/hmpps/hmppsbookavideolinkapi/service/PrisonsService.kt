package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel

@Service
class PrisonsService(private val prisonRepository: PrisonRepository) {
  fun getListOfPrisons(enabledOnly: Boolean): List<Prison> {
    return if (!enabledOnly) {
      prisonRepository.findAll().toModel()
    } else {
      prisonRepository.findAllByEnabledIsTrue().toModel()
    }
  }
}

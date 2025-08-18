package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendPrisonRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel

@Service
class PrisonsService(private val prisonRepository: PrisonRepository) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getListOfPrisons(enabledOnly: Boolean): List<Prison> = if (!enabledOnly) {
    prisonRepository.findAll().toModel()
  } else {
    prisonRepository.findAllByEnabledIsTrue().toModel()
  }

  fun getPrison(prisonCode: String) = prisonRepository.findByCode(prisonCode)?.toModel()
    ?: throw EntityNotFoundException("Prison with code $prisonCode not found.")

  fun amend(prisonCode: String, request: AmendPrisonRequest): Prison {
    val prison = prisonRepository.findByCode(prisonCode)

    if (prison == null) {
      throw EntityNotFoundException("Prison with code $prisonCode not found.")
    }

    prisonRepository.saveAndFlush(prison.apply { pickUpTime = request.pickUpTime })

    return prison.toModel().also {
      log.info("Amended prison with code $prisonCode with a pick-up time of ${request.pickUpTime}")
    }
  }
}

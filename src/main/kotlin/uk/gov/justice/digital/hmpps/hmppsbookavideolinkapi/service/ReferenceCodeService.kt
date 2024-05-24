package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ReferenceCodeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel

@Service
class ReferenceCodeService(private val referenceCodeRepository: ReferenceCodeRepository) {
  fun getReferenceDataByGroup(groupCode: String): List<ReferenceCode> =
    referenceCodeRepository.findAllByGroupCodeEquals(groupCode).toModel()
}

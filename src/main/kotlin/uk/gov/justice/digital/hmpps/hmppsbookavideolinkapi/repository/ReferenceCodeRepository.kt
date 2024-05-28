package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ReferenceCode

@Repository
interface ReferenceCodeRepository : JpaRepository<ReferenceCode, Long> {
  fun findAllByGroupCodeEquals(groupCode: String): List<ReferenceCode>
}

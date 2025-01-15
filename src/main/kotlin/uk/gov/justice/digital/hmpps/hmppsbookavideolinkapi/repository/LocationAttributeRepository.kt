package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationAttribute

@Repository
interface LocationAttributeRepository : JpaRepository<LocationAttribute, Long> {
  fun findByPrisonCode(prisonCode: String): List<LocationAttribute>
}

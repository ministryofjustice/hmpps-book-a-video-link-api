package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationAttribute
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationStatus
import java.util.UUID

@Repository
interface LocationAttributeRepository : JpaRepository<LocationAttribute, Long> {
  fun findByPrisonCode(prisonCode: String): List<LocationAttribute>

  fun findByDpsLocationIdAndLocationStatus(uuid: UUID, locationStatus: LocationStatus): LocationAttribute?
}

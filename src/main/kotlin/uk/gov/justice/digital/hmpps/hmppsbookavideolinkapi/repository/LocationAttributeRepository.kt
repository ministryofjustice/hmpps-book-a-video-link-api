package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationAttribute
import java.time.LocalDate
import java.util.UUID

@Repository
interface LocationAttributeRepository : JpaRepository<LocationAttribute, Long> {
  fun findByPrisonCode(prisonCode: String): List<LocationAttribute>

  fun findByDpsLocationId(uuid: UUID): LocationAttribute?

  fun deleteLocationAttributesByDpsLocationId(dpsLocationId: UUID)

  fun findByBlockedToNotNullAndBlockedToIsBefore(date: LocalDate): List<LocationAttribute>
}

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationSchedule
import java.util.UUID

interface LocationScheduleRepository : JpaRepository<LocationSchedule, Long> {
  @Query(value = "FROM LocationSchedule ls WHERE ls.locationScheduleId = :locationScheduleId AND ls.locationAttribute.dpsLocationId = :dpsLocationId")
  fun findByScheduleIdAndDpsLocationId(locationScheduleId: Long, dpsLocationId: UUID): LocationSchedule?
}

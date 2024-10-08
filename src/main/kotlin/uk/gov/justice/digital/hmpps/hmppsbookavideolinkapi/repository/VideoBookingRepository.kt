package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking

@Repository
interface VideoBookingRepository : JpaRepository<VideoBooking, Long> {
  fun existsByMigratedVideoBookingId(id: Long): Boolean
}

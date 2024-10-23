package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking

@Repository
interface VideoBookingRepository : JpaRepository<VideoBooking, Long> {
  fun existsByMigratedVideoBookingId(id: Long): Boolean

  @Query(
    value = """
      SELECT count(distinct v)
      FROM VideoBooking v
      JOIN VideoAppointment va on va.videoBookingId = v.videoBookingId
      WHERE va.prisonerNumber = :prisonerNumber
    """,
  )
  fun countDistinctByPrisonerNumber(prisonerNumber: String): Long
}

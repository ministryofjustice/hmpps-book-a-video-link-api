package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import java.time.LocalDate
import java.util.UUID

@Repository
interface VideoBookingRepository : JpaRepository<VideoBooking, Long> {
  @Query(
    value = """
      SELECT count(distinct v)
      FROM VideoBooking v
      JOIN VideoAppointment va on va.videoBookingId = v.videoBookingId
      WHERE va.prisonerNumber = :prisonerNumber
    """,
  )
  fun countDistinctByPrisonerNumber(prisonerNumber: String): Long

  @Query(
    value = """
      SELECT vb.videoBookingId from VideoBooking vb
      JOIN PrisonAppointment pa on pa.videoBooking = vb
      WHERE pa.appointmentType = :appointmentType
      AND pa.appointmentDate BETWEEN :fromDate AND :toDate
      AND (:prisonCode IS NULL OR pa.prison.code = :prisonCode)
      AND (:prisonerNumber IS NULL OR pa.prisonerNumber = :prisonerNumber)
      AND (:locationId IS NULL OR pa.prisonLocationId = :locationId)
    """,
  )
  fun findBookingIdsPaginated(appointmentType: String, fromDate: LocalDate, toDate: LocalDate, prisonCode: String?, prisonerNumber: String?, locationId: UUID?, pageable: Pageable): Page<Long>
}

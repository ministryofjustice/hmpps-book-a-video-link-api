package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.AdditionalBookingDetail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import java.time.LocalDate

interface AdditionalBookingDetailRepository : JpaRepository<AdditionalBookingDetail, Long> {
  fun findByVideoBooking(booking: VideoBooking): AdditionalBookingDetail?

  fun existsByVideoBooking(booking: VideoBooking): Boolean

  @Query(
    value = """
    SELECT abd FROM AdditionalBookingDetail abd 
      JOIN PrisonAppointment pa ON pa.videoBooking = abd.videoBooking
     WHERE pa.videoBooking.statusCode = 'ACTIVE'
       AND pa.appointmentDate = :date
       AND pa.appointmentType = 'VLB_PROBATION'
  """,
  )
  fun findAdditionalDetailsForAllActiveProbationMeetingsOnDate(date: LocalDate): List<AdditionalBookingDetail>
}

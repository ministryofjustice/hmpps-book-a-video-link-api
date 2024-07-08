package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import java.time.LocalDate
import java.time.LocalTime

interface PrisonAppointmentRepository : JpaRepository<PrisonAppointment, Long> {
  fun findByVideoBooking(booking: VideoBooking): List<PrisonAppointment>

  fun findByPrisonCodeAndPrisonLocKeyAndAppointmentDate(
    prisonCode: String,
    key: String,
    date: LocalDate,
  ): List<PrisonAppointment>

  @Query(
    value = """
    SELECT pa FROM PrisonAppointment pa 
     WHERE pa.prisonerNumber = :prisonerNumber
       AND pa.videoBooking.statusCode = 'ACTIVE'
       AND ((pa.appointmentDate = :date AND pa.startTime > :time) OR (pa.appointmentDate > :date))
  """,
  )
  fun findActivePrisonerPrisonAppointmentsAfter(
    prisonerNumber: String,
    date: LocalDate,
    time: LocalTime,
  ): List<PrisonAppointment>
}

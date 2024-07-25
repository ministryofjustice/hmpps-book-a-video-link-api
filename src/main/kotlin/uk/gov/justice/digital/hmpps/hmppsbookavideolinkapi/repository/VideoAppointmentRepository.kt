package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoAppointment
import java.time.LocalDate
import java.time.LocalTime

@Repository
interface VideoAppointmentRepository : ReadOnlyRepository<VideoAppointment, Long> {
  @Query(
    value = """
      FROM VideoAppointment va 
      WHERE va.appointmentDate = :forDate
      AND va.prisonCode  = :forPrison
      AND va.prisonLocKey in (:forLocationKeys)
      AND va.statusCode = 'ACTIVE'
      ORDER BY va.prisonLocKey, va.startTime
    """,
  )
  fun findVideoAppointmentsAtPrison(
    forDate: LocalDate,
    forPrison: String,
    forLocationKeys: List<String>,
  ): List<VideoAppointment>

  fun findByPrisonerNumberAndAppointmentDateAndPrisonLocKeyAndStartTimeAndEndTime(
    prisonerNumber: String,
    appointmentDate: LocalDate,
    prisonLocKey: String,
    startTime: LocalTime,
    endTime: LocalTime,
  ): VideoAppointment?
}

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
      AND va.prisonLocationId in (:forLocationIds)
      AND va.statusCode = 'ACTIVE'
      ORDER BY va.prisonLocationId, va.startTime
    """,
  )
  fun findVideoAppointmentsAtPrison(
    forDate: LocalDate,
    forPrison: String,
    forLocationIds: List<String>,
  ): List<VideoAppointment>

  @Query(
    value = """
      FROM VideoAppointment va 
      WHERE va.prisonerNumber = :prisonerNumber
      AND va.appointmentDate = :appointmentDate
      AND va.prisonLocationId = :prisonLocationId
      AND va.startTime = :startTime
      AND va.endTime = :endTime
      AND va.statusCode = 'ACTIVE'
    """,
  )
  fun findActiveVideoAppointment(
    prisonerNumber: String,
    appointmentDate: LocalDate,
    prisonLocationId: String,
    startTime: LocalTime,
    endTime: LocalTime,
  ): VideoAppointment?
}

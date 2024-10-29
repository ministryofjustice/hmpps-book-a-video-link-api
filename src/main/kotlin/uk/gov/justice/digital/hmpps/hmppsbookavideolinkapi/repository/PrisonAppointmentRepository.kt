package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

interface PrisonAppointmentRepository : JpaRepository<PrisonAppointment, Long> {
  fun findByVideoBooking(booking: VideoBooking): List<PrisonAppointment>

  @Query(
    value = """
    SELECT CASE WHEN count(pa) > 0 THEN TRUE ELSE FALSE END
      FROM PrisonAppointment pa 
     WHERE pa.prisonerNumber = :prisonerNumber
       AND pa.prison.code = :prisonCode
       AND pa.prisonLocUuid = :locationId
       AND pa.videoBooking.statusCode = 'ACTIVE'
       AND pa.appointmentDate = :date
       AND pa.startTime = :startTime
       and pa.endTime = :endTime
  """,
  )
  fun existsActivePrisonAppointmentsByPrisonerNumberLocationDateAndTime(
    prisonerNumber: String,
    prisonCode: String,
    locationId: UUID,
    date: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime,
  ): Boolean

  @Query(
    value = """
    SELECT pa FROM PrisonAppointment pa 
     WHERE pa.videoBooking.statusCode = 'ACTIVE'
       AND pa.appointmentDate = :date
  """,
  )
  fun findAllActivePrisonAppointmentsOnDate(date: LocalDate): List<PrisonAppointment>

  @Query(
    value = """
    SELECT pa FROM PrisonAppointment pa 
     WHERE pa.prison.code = :prisonCode
       AND pa.prisonLocUuid = :locationId
       AND pa.videoBooking.statusCode = 'ACTIVE'
       AND pa.appointmentDate = :date
  """,
  )
  fun findActivePrisonAppointmentsAtLocationOnDate(
    prisonCode: String,
    locationId: UUID,
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

  fun countByPrisonerNumber(prisonerNumber: String): Long

  @Query(value = "UPDATE PrisonAppointment pa SET pa.prisonerNumber = :replacementNumber WHERE pa.prisonerNumber = :removedNumber")
  @Modifying
  fun mergePrisonerNumber(removedNumber: String, replacementNumber: String)
}

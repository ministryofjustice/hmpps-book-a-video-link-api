package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ScheduleItem
import java.time.LocalDate

@Repository
interface ScheduleRepository : ReadOnlyRepository<ScheduleItem, Long> {
  @Query(
    value = """
      FROM ScheduleItem si 
      WHERE si.appointmentDate = :date
      AND si.prisonCode  = :prisonCode
      AND si.statusCode = "ACTIVE"
      ORDER BY si.appointmentDate, si.startTime
    """,
  )
  fun getScheduleForPrison(prisonCode: String, date: LocalDate): List<ScheduleItem>

  @Query(
    value = """
      FROM ScheduleItem si 
      WHERE si.appointmentDate = :date
      AND si.prisonCode  = :prisonCode
      ORDER BY si.appointmentDate, si.startTime
    """,
  )
  fun getScheduleForPrisonIncludingCancelled(prisonCode: String, date: LocalDate): List<ScheduleItem>

  @Query(
    value = """
      FROM ScheduleItem si 
      WHERE si.appointmentDate = :date
      AND si.bookingType = "COURT"
      AND si.courtCode = :courtCode
      AND si.statusCode = "ACTIVE"
      ORDER BY si.appointmentDate, si.startTime
    """,
  )
  fun getScheduleForCourt(courtCode: String, date: LocalDate): List<ScheduleItem>

  @Query(
    value = """
      FROM ScheduleItem si 
      WHERE si.appointmentDate = :date
      AND si.bookingType = "COURT"
      AND si.courtCode = :courtCode
      ORDER BY si.appointmentDate, si.startTime
    """,
  )
  fun getScheduleForCourtIncludingCancelled(courtCode: String, date: LocalDate): List<ScheduleItem>

  @Query(
    value = """
      FROM ScheduleItem si 
      WHERE si.appointmentDate = :date
      AND si.bookingType = "PROBATION"
      AND si.probationTeamCode = :probationTeamCode
      AND si.statusCode = "ACTIVE"
      ORDER BY si.appointmentDate, si.startTime
    """,
  )
  fun getScheduleForProbationTeam(probationTeamCode: String, date: LocalDate): List<ScheduleItem>

  @Query(
    value = """
      FROM ScheduleItem si 
      WHERE si.appointmentDate = :date
      AND si.bookingType = "PROBATION"
      AND si.probationTeamCode = :probationTeamCode
      ORDER BY si.appointmentDate, si.startTime
    """,
  )
  fun getScheduleForProbationTeamIncludingCancelled(probationTeamCode: String, date: LocalDate): List<ScheduleItem>
}

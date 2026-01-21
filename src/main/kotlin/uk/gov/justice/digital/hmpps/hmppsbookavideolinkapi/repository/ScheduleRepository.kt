package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ScheduleItem
import java.time.LocalDate

@Repository
interface ScheduleRepository : JpaRepository<ScheduleItem, Long> {
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

  @Query(
    value = """
      FROM ScheduleItem si
      WHERE si.appointmentDate = :date
      AND si.bookingType = "COURT"
      AND si.courtCode in :courtCodes
      AND si.statusCode = "ACTIVE"
    """,
  )
  fun getScheduleForCourtsPaginated(courtCodes: List<String>, date: LocalDate, pageable: Pageable): Page<ScheduleItem>

  @Query(
    value = """
      FROM ScheduleItem si
      WHERE si.appointmentDate = :date
      AND si.bookingType = "PROBATION"
      AND si.probationTeamCode in :probationTeamCodes
      AND si.statusCode = "ACTIVE"
    """,
  )
  fun getScheduleForProbationTeamsPaginated(probationTeamCodes: List<String>, date: LocalDate, pageable: Pageable): Page<ScheduleItem>

  @Query(
    value = """
      FROM ScheduleItem si
      WHERE si.appointmentDate = :date
      AND si.bookingType = "COURT"
      AND si.courtCode in :courtCodes
      AND si.statusCode = "ACTIVE"
    """,
  )
  fun getScheduleForCourtsUnpaginated(courtCodes: List<String>, date: LocalDate, sort: Sort): List<ScheduleItem>

  @Query(
    value = """
      FROM ScheduleItem si
      WHERE si.appointmentDate = :date
      AND si.bookingType = "PROBATION"
      AND si.probationTeamCode in :probationTeamCodes
      AND si.statusCode = "ACTIVE"
    """,
  )
  fun getScheduleForProbationTeamsUnpaginated(probationTeamCodes: List<String>, date: LocalDate, sort: Sort): List<ScheduleItem>
}

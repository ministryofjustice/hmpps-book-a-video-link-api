package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ScheduleItem
import java.time.LocalDate

@Repository
interface PrisonScheduleRepository : JpaRepository<ScheduleItem, Long> {
  @Query(
    value = """
      FROM ScheduleItem si 
      WHERE si.appointmentDate = :forDate
      AND si.prisonCode  = :forPrison
      AND si.statusCode != "CANCELLED"
      ORDER BY si.appointmentDate, si.startTime
    """,
  )
  fun getSchedule(forDate: LocalDate, forPrison: String): List<ScheduleItem>

  @Query(
    value = """
      FROM ScheduleItem si 
      WHERE si.appointmentDate = :forDate
      AND si.prisonCode  = :forPrison
      ORDER BY si.appointmentDate, si.startTime
    """,
  )
  fun getScheduleIncludingCancelled(forDate: LocalDate, forPrison: String): List<ScheduleItem>
}

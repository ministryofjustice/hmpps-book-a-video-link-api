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
      WHERE ((CAST(:toDate as date) is null AND si.appointmentDate = :fromDate) OR si.appointmentDate between :fromDate and :toDate)
      AND si.bookingType = "COURT"
      AND si.courtCode in :courtCodes
      AND si.statusCode = "ACTIVE"
      AND (:excludingPrisons is null or si.prisonCode not in (:excludingPrisons))
      AND (:includingPrisonerNumbers is null or si.prisonerNumber in (:includingPrisonerNumbers))
      AND (:hasCourtHearingLink is distinct from true or coalesce(si.hmctsNumber, si.videoUrl) is not null)
      AND (:hasCourtHearingLink is distinct from false or coalesce(si.hmctsNumber, si.videoUrl) is null)
      AND EXISTS (SELECT 1 FROM Prison p WHERE p.code = si.prisonCode AND p.enabled = true)
    """,
  )
  fun getScheduleForCourtsPaginated(courtCodes: List<String>, fromDate: LocalDate, toDate: LocalDate?, excludingPrisons: List<String>?, includingPrisonerNumbers: List<String>?, hasCourtHearingLink: Boolean?, pageable: Pageable): Page<ScheduleItem>

  @Query(
    value = """
      FROM ScheduleItem si
      WHERE ((CAST(:toDate as date) is null AND si.appointmentDate = :fromDate) OR si.appointmentDate between :fromDate and :toDate)
      AND si.bookingType = "PROBATION"
      AND si.probationTeamCode in :probationTeamCodes
      AND si.statusCode = "ACTIVE"
      AND (:excludingPrisons is null or si.prisonCode not in (:excludingPrisons))
      AND (:includingPrisonerNumbers is null or si.prisonerNumber in (:includingPrisonerNumbers))
      AND (:hasProbationOfficerDetails is distinct from true or si.probationOfficerName is not null)
      AND (:hasProbationOfficerDetails is distinct from false or si.probationOfficerName is null)
      AND EXISTS (SELECT 1 FROM Prison p WHERE p.code = si.prisonCode AND p.enabled = true)
    """,
  )
  fun getScheduleForProbationTeamsPaginated(probationTeamCodes: List<String>, fromDate: LocalDate, toDate: LocalDate?, excludingPrisons: List<String>?, includingPrisonerNumbers: List<String>?, hasProbationOfficerDetails: Boolean?, pageable: Pageable): Page<ScheduleItem>

  @Query(
    value = """
      FROM ScheduleItem si
      WHERE ((CAST(:toDate as date) is null AND si.appointmentDate = :fromDate) OR si.appointmentDate between :fromDate and :toDate)
      AND si.bookingType = "COURT"
      AND si.courtCode in :courtCodes
      AND si.statusCode = "ACTIVE"
      AND (:excludingPrisons is null or si.prisonCode not in (:excludingPrisons))
      AND (:includingPrisonerNumbers is null or si.prisonerNumber in (:includingPrisonerNumbers))
      AND (:hasCourtHearingLink is distinct from true or coalesce(si.hmctsNumber, si.videoUrl) is not null)
      AND (:hasCourtHearingLink is distinct from false or coalesce(si.hmctsNumber, si.videoUrl) is null)
      AND EXISTS (SELECT 1 FROM Prison p WHERE p.code = si.prisonCode AND p.enabled = true)
    """,
  )
  fun getScheduleForCourtsUnpaginated(courtCodes: List<String>, fromDate: LocalDate, toDate: LocalDate?, excludingPrisons: List<String>?, includingPrisonerNumbers: List<String>?, hasCourtHearingLink: Boolean?, sort: Sort): List<ScheduleItem>

  @Query(
    value = """
      FROM ScheduleItem si
      WHERE ((CAST(:toDate as date) is null AND si.appointmentDate = :fromDate) OR si.appointmentDate between :fromDate and :toDate)
      AND si.bookingType = "PROBATION"
      AND si.probationTeamCode in :probationTeamCodes
      AND si.statusCode = "ACTIVE"
      AND (:excludingPrisons is null or si.prisonCode not in (:excludingPrisons))
      AND (:includingPrisonerNumbers is null or si.prisonerNumber in (:includingPrisonerNumbers))
      AND (:hasProbationOfficerDetails is distinct from true or si.probationOfficerName is not null)
      AND (:hasProbationOfficerDetails is distinct from false or si.probationOfficerName is null)
      AND EXISTS (SELECT 1 FROM Prison p WHERE p.code = si.prisonCode AND p.enabled = true)
    """,
  )
  fun getScheduleForProbationTeamsUnpaginated(probationTeamCodes: List<String>, fromDate: LocalDate, toDate: LocalDate?, excludingPrisons: List<String>?, includingPrisonerNumbers: List<String>?, hasProbationOfficerDetails: Boolean?, sort: Sort): List<ScheduleItem>
}

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationTeam
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class LocationScheduleTest {
  private val saturday = LocalDate.of(2025, 2, 22).atStartOfDay()
  private val sunday = LocalDate.of(2025, 2, 23).atStartOfDay()
  private val mondayNoon = LocalDate.of(2025, 2, 24).atTime(12, 0)
  private val mondayMidnight = LocalDate.of(2025, 2, 24).atTime(0, 0)

  @DisplayName("Probation tests")
  @Nested
  inner class Probation {
    @Test
    fun `should be available for probation team on Monday at noon`() {
      val schedule = schedule(locationUsage = LocationUsage.PROBATION, allowedParties = "PROBATION_TEAM")

      schedule.isAvailableForProbationTeam(probationTeam(code = "PROBATION_TEAM"), mondayNoon) isBool true
    }

    @Test
    fun `should be available for probation team on Monday out of hours`() {
      val schedule = schedule(locationUsage = LocationUsage.PROBATION, allowedParties = "PROBATION_TEAM")

      schedule.isAvailableForProbationTeam(probationTeam(code = "PROBATION_TEAM"), mondayMidnight) isBool true
    }

    @Test
    fun `should be not available for probation team on Monday at noon`() {
      val schedule = schedule(locationUsage = LocationUsage.PROBATION, allowedParties = "PROBATION_TEAM")

      schedule.isAvailableForProbationTeam(probationTeam(code = "DIFFERENT_PROBATION_TEAM"), mondayNoon) isBool false
    }

    @Test
    fun `should be available for any probation team on Monday at noon`() {
      val schedule = schedule(locationUsage = LocationUsage.PROBATION)

      schedule.isAvailableForAnyProbationTeam(mondayNoon) isBool true
    }

    @Test
    fun `should be available for any probation team on Monday out of hours`() {
      val schedule = schedule(locationUsage = LocationUsage.PROBATION)

      schedule.isAvailableForAnyProbationTeam(mondayMidnight) isBool true
    }

    @Test
    fun `should not be available for any probation team on Monday at noon`() {
      val schedule = schedule(locationUsage = LocationUsage.PROBATION, allowedParties = "PROBATION_TEAM")

      schedule.isAvailableForAnyProbationTeam(mondayNoon) isBool false
    }

    @Test
    fun `should be available for any probation team on Saturday or Sunday`() {
      val schedule = schedule(locationUsage = LocationUsage.PROBATION)

      schedule.isAvailableForAnyProbationTeam(saturday) isBool true
      schedule.isAvailableForAnyProbationTeam(sunday) isBool true
    }
  }

  private fun schedule(
    locationUsage: LocationUsage,
    start: DayOfWeek = DayOfWeek.MONDAY,
    end: DayOfWeek = DayOfWeek.FRIDAY,
    startTime: LocalTime = LocalTime.of(9, 0),
    endTime: LocalTime = LocalTime.of(17, 0),
    allowedParties: String? = null,
  ) = LocationSchedule(
    locationScheduleId = 1,
    locationAttribute = LocationAttribute(
      locationAttributeId = 1L,
      dpsLocationId = UUID.randomUUID(),
      prison = Prison(
        prisonId = 1,
        code = PENTONVILLE,
        name = "TEST",
        enabled = true,
        createdBy = "TEST",
        notes = null,
      ),
      locationStatus = LocationStatus.ACTIVE,
      locationUsage = LocationUsage.SCHEDULE,
      createdBy = "TEST",
    ),
    startDayOfWeek = start.value,
    endDayOfWeek = end.value,
    startTime = startTime,
    endTime = endTime,
    locationUsage = locationUsage,
    allowedParties = allowedParties,
    notes = null,
    createdBy = "test",
  )
}

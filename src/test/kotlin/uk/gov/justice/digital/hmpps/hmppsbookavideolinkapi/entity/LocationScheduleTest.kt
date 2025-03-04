package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvillePrison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationTeam
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class LocationScheduleTest {
  private val mondayNoon = LocalDate.of(2025, 2, 24).atTime(12, 0)
  private val tuesdayNoon = mondayNoon.plusDays(1)
  private val wednesdayNoon = mondayNoon.plusDays(2)
  private val thursdayNoon = mondayNoon.plusDays(3)
  private val fridayNoon = mondayNoon.plusDays(4)

  @Test
  fun `should fall on date and time`() {
    schedule(start = DayOfWeek.MONDAY).fallsOn(mondayNoon) isBool true
    schedule(start = DayOfWeek.TUESDAY).fallsOn(tuesdayNoon) isBool true
    schedule(start = DayOfWeek.WEDNESDAY).fallsOn(wednesdayNoon) isBool true
    schedule(start = DayOfWeek.THURSDAY).fallsOn(thursdayNoon) isBool true
    schedule(start = DayOfWeek.FRIDAY).fallsOn(fridayNoon) isBool true
  }

  @Test
  fun `should not fall on date and time`() {
    schedule(start = DayOfWeek.MONDAY).fallsOn(tuesdayNoon) isBool false
    schedule(start = DayOfWeek.MONDAY).fallsOn(wednesdayNoon) isBool false
    schedule(start = DayOfWeek.MONDAY).fallsOn(thursdayNoon) isBool false
    schedule(start = DayOfWeek.MONDAY).fallsOn(fridayNoon) isBool false
  }

  @Test
  fun `should fail if location usage SCHEDULE`() {
    assertThrows<IllegalArgumentException> { schedule(locationUsage = LocationUsage.SCHEDULE) }.message isEqualTo "The location usage cannot be of type SCHEDULE for a location schedule row."
  }

  @Test
  fun `should fail if end day before start day`() {
    assertThrows<IllegalArgumentException> { schedule(start = DayOfWeek.TUESDAY, end = DayOfWeek.MONDAY) }.message isEqualTo "The end day cannot be before the start day."
  }

  @Test
  fun `should fail if end time not after start time`() {
    assertThrows<IllegalArgumentException> { schedule(startTime = LocalTime.of(12, 0), endTime = LocalTime.of(11, 59)) }.message isEqualTo "The end time must come after the start time."
  }

  @DisplayName("Probation tests")
  @Nested
  inner class Probation {
    @Test
    fun `should be for probation team`() {
      val schedule = schedule(locationUsage = LocationUsage.PROBATION, allowedParties = "PROBATION_TEAM")

      schedule.isForProbationTeam(probationTeam(code = "PROBATION_TEAM")) isBool true
    }

    @Test
    fun `should not be for probation team`() {
      val schedule = schedule(locationUsage = LocationUsage.PROBATION, allowedParties = "PROBATION_TEAM")

      schedule.isForProbationTeam(probationTeam(code = "DIFFERENT_PROBATION_TEAM")) isBool false
    }

    @Test
    fun `should be for any probation team`() {
      val schedule = schedule(locationUsage = LocationUsage.PROBATION)

      schedule.isForAnyProbationTeam() isBool true
    }

    @Test
    fun `should not be for any probation team`() {
      val schedule = schedule(locationUsage = LocationUsage.COURT)

      schedule.isForAnyProbationTeam() isBool false
    }
  }

  private fun schedule(
    locationUsage: LocationUsage = LocationUsage.SHARED,
    start: DayOfWeek = DayOfWeek.MONDAY,
    end: DayOfWeek = start,
    startTime: LocalTime = LocalTime.of(9, 0),
    endTime: LocalTime = LocalTime.of(17, 0),
    allowedParties: String? = null,
  ) = LocationSchedule(
    locationScheduleId = 1,
    locationAttribute = LocationAttribute.decoratedRoom(
      dpsLocationId = UUID.randomUUID(),
      prison = pentonvillePrison,
      locationStatus = LocationStatus.ACTIVE,
      locationUsage = LocationUsage.SCHEDULE,
      allowedParties = emptySet(),
      prisonVideoUrl = null,
      createdBy = PROBATION_USER,
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

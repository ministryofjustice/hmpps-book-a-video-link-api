package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvillePrison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationTeam
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
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
  fun `should fail if end day before start day`() {
    assertThrows<IllegalArgumentException> {
      schedule(
        start = DayOfWeek.TUESDAY,
        end = DayOfWeek.MONDAY,
      )
    }.message isEqualTo "The end day cannot be before the start day."

    assertThrows<IllegalArgumentException> {
      schedule().amend(
        locationUsage = LocationScheduleUsage.PROBATION,
        startDayOfWeek = 2,
        endDayOfWeek = 1,
        startTime = LocalTime.now(),
        endTime = LocalTime.now().plusMinutes(1),
        allowedParties = null,
        notes = null,
        amendedBy = PROBATION_USER,
      )
    }.message isEqualTo "The end day cannot be before the start day."
  }

  @Test
  fun `should fail if end time not after start time`() {
    assertThrows<IllegalArgumentException> {
      schedule(
        startTime = LocalTime.of(12, 0),
        endTime = LocalTime.of(11, 59),
      )
    }.message isEqualTo "The end time must come after the start time."

    assertThrows<IllegalArgumentException> {
      schedule().amend(
        locationUsage = LocationScheduleUsage.PROBATION,
        startDayOfWeek = 1,
        endDayOfWeek = 2,
        startTime = LocalTime.now(),
        endTime = LocalTime.now().minusMinutes(1),
        allowedParties = null,
        notes = null,
        amendedBy = PROBATION_USER,
      )
    }.message isEqualTo "The end time must come after the start time."
  }

  @Test
  fun `should fail to amend if causes a duplicate row`() {
    val schedule = schedule(
      start = DayOfWeek.MONDAY,
      end = DayOfWeek.FRIDAY,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(15, 0),
      locationUsage = LocationScheduleUsage.PROBATION,
      allowedParties = setOf("PROBATION_TEAM"),
    )

    val locationAttribute = schedule.locationAttribute

    locationAttribute.addSchedule(
      usage = LocationScheduleUsage.PROBATION,
      startDayOfWeek = DayOfWeek.MONDAY.value,
      endDayOfWeek = DayOfWeek.SUNDAY.value,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(15, 0),
      allowedParties = setOf("PROBATION_TEAM"),
      notes = null,
      createdBy = PROBATION_USER,
    )

    assertThrows<IllegalArgumentException> {
      schedule.amend(
        locationUsage = LocationScheduleUsage.PROBATION,
        startDayOfWeek = DayOfWeek.MONDAY.value,
        endDayOfWeek = DayOfWeek.SUNDAY.value,
        startTime = LocalTime.of(12, 0),
        endTime = LocalTime.of(15, 0),
        allowedParties = setOf("PROBATION_TEAM"),
        notes = null,
        amendedBy = PROBATION_USER,
      )
    }.message isEqualTo "Cannot amend, amendment would conflict with an existing scheduled row."
  }

  @Test
  fun `should amend row`() {
    val schedule = schedule(
      start = DayOfWeek.MONDAY,
      end = DayOfWeek.FRIDAY,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(15, 0),
      locationUsage = LocationScheduleUsage.PROBATION,
      allowedParties = setOf("PROBATION_TEAM"),
    )

    val locationAttribute = schedule.locationAttribute

    locationAttribute.addSchedule(
      usage = LocationScheduleUsage.PROBATION,
      startDayOfWeek = DayOfWeek.MONDAY.value,
      endDayOfWeek = DayOfWeek.SUNDAY.value,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(15, 0),
      allowedParties = setOf("PROBATION_TEAM"),
      notes = null,
      createdBy = PROBATION_USER,
    )

    schedule.amend(
      locationUsage = LocationScheduleUsage.PROBATION,
      startDayOfWeek = DayOfWeek.MONDAY.value,
      endDayOfWeek = DayOfWeek.WEDNESDAY.value,
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(14, 0),
      allowedParties = setOf("ANOTHER_PROBATION_TEAM"),
      notes = null,
      amendedBy = PROBATION_USER,
    )

    with(schedule) {
      locationUsage isEqualTo LocationScheduleUsage.PROBATION
      startDayOfWeek isEqualTo DayOfWeek.MONDAY.value
      endDayOfWeek isEqualTo DayOfWeek.WEDNESDAY.value
      startTime isEqualTo LocalTime.of(11, 0)
      endTime isEqualTo LocalTime.of(14, 0)
      allowedParties isEqualTo "ANOTHER_PROBATION_TEAM"
      amendedBy isEqualTo PROBATION_USER.username
      amendedTime isCloseTo LocalDateTime.now()
    }
  }

  @DisplayName("Probation tests")
  @Nested
  inner class Probation {
    @Test
    fun `should be for probation team`() {
      val schedule = schedule(locationUsage = LocationScheduleUsage.PROBATION, allowedParties = setOf("PROBATION_TEAM"))

      schedule.isForProbationTeam(probationTeam(code = "PROBATION_TEAM")) isBool true
    }

    @Test
    fun `should not be for probation team`() {
      val schedule = schedule(locationUsage = LocationScheduleUsage.PROBATION, allowedParties = setOf("PROBATION_TEAM"))

      schedule.isForProbationTeam(probationTeam(code = "DIFFERENT_PROBATION_TEAM")) isBool false
    }

    @Test
    fun `should be for any probation team`() {
      val schedule = schedule(locationUsage = LocationScheduleUsage.PROBATION)

      schedule.isForAnyProbationTeam() isBool true
    }

    @Test
    fun `should not be for any probation team`() {
      val schedule = schedule(locationUsage = LocationScheduleUsage.COURT)

      schedule.isForAnyProbationTeam() isBool false
    }
  }

  @DisplayName("Court tests")
  @Nested
  inner class Court {
    @Test
    fun `should be for probation team`() {
      val schedule = schedule(locationUsage = LocationScheduleUsage.COURT, allowedParties = setOf("COURT"))

      schedule.isForCourt(court(code = "COURT")) isBool true
    }

    @Test
    fun `should not be for probation team`() {
      val schedule = schedule(locationUsage = LocationScheduleUsage.COURT, allowedParties = setOf("COURT"))

      schedule.isForCourt(court(code = "DIFFERENT_COURT")) isBool false
    }

    @Test
    fun `should be for any probation team`() {
      val schedule = schedule(locationUsage = LocationScheduleUsage.COURT)

      schedule.isForAnyCourt() isBool true
    }

    @Test
    fun `should not be for any probation team`() {
      val schedule = schedule(locationUsage = LocationScheduleUsage.PROBATION)

      schedule.isForAnyCourt() isBool false
    }
  }

  private fun schedule(
    locationUsage: LocationScheduleUsage = LocationScheduleUsage.PROBATION,
    start: DayOfWeek = DayOfWeek.MONDAY,
    end: DayOfWeek = start,
    startTime: LocalTime = LocalTime.of(9, 0),
    endTime: LocalTime = LocalTime.of(17, 0),
    allowedParties: Set<String>? = null,
  ) = LocationSchedule.newSchedule(
    locationAttribute = LocationAttribute.decoratedRoom(
      dpsLocationId = UUID.randomUUID(),
      prison = pentonvillePrison,
      locationStatus = LocationStatus.ACTIVE,
      locationUsage = LocationUsage.SCHEDULE,
      allowedParties = emptySet(),
      prisonVideoUrl = null,
      notes = null,
      createdBy = PROBATION_USER,
    ),
    startDayOfWeek = start.value,
    endDayOfWeek = end.value,
    startTime = startTime,
    endTime = endTime,
    locationUsage = locationUsage,
    allowedParties = allowedParties,
    notes = null,
    createdBy = COURT_USER,
  )
}

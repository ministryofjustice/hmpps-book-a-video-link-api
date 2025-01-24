package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

class LocationAttributesTest {
  @Test
  fun `should reject location attributes with a schedule if the location usage is not SCHEDULE`() {
    val roomAttributes = LocationAttribute(
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
      locationUsage = LocationUsage.COURT,
      createdBy = "TEST",
    )

    val exception = assertThrows<IllegalArgumentException> {
      mutableListOf(
        LocationSchedule(
          locationScheduleId = 1,
          startDayOfWeek = DayOfWeek.MONDAY.value,
          endDayOfWeek = DayOfWeek.SUNDAY.value,
          startTime = LocalTime.of(1, 0),
          endTime = LocalTime.of(23, 0),
          locationUsage = LocationUsage.SHARED,
          allowedParties = null,
          createdBy = "TEST",
          locationAttribute = roomAttributes,
        ),
      )
    }

    exception.message isEqualTo "The location usage type must be SCHEDULE for a list of schedule rows to be associated with it."
  }

  @Test
  fun `should accept attributes with a schedule if location usage is SCHEDULE`() {
    val roomAttributes = LocationAttribute(
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
    )

    val roomSchedule = mutableListOf(
      LocationSchedule(
        locationScheduleId = 1,
        startDayOfWeek = DayOfWeek.MONDAY.value,
        endDayOfWeek = DayOfWeek.SUNDAY.value,
        startTime = LocalTime.of(1, 0),
        endTime = LocalTime.of(23, 0),
        locationUsage = LocationUsage.SHARED,
        allowedParties = null,
        createdBy = "TEST",
        locationAttribute = roomAttributes,
      ),
    )

    roomAttributes.setLocationSchedule(roomSchedule)

    assertThat(roomAttributes.schedule().hasSize(1))
  }
}

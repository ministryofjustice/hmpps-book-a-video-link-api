package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationTeam
import java.time.DayOfWeek
import java.time.LocalDateTime
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

    val roomSchedule = listOf(
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

  @Nested
  inner class Probation {
    @Test
    fun `should be available for active probation room attribute`() {
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
        locationUsage = LocationUsage.PROBATION,
        createdBy = "TEST",
      )

      roomAttributes.isAvailableFor(probationTeam(), LocalDateTime.now()) isBool true
    }

    @Test
    fun `should not be available for inactive probation room attribute`() {
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
        locationStatus = LocationStatus.INACTIVE,
        locationUsage = LocationUsage.PROBATION,
        createdBy = "TEST",
      )

      roomAttributes.isAvailableFor(probationTeam(), LocalDateTime.now()) isBool false
    }

    @Test
    fun `should be available for specific active probation room attribute`() {
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
        locationUsage = LocationUsage.PROBATION,
        createdBy = "TEST",
        allowedParties = "TEAM_CODE",
      )

      roomAttributes.isAvailableFor(probationTeam(code = "TEAM_CODE"), LocalDateTime.now()) isBool true
    }

    @Test
    fun `should not be available for specific active probation room attribute`() {
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
        locationUsage = LocationUsage.PROBATION,
        createdBy = "TEST",
        allowedParties = "TEAM_CODE",
      )

      roomAttributes.isAvailableFor(probationTeam(code = "DIFFERENT_TEAM_CODE"), LocalDateTime.now()) isBool false
    }

    @Test
    fun `should not be available for active court room attribute`() {
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

      roomAttributes.isAvailableFor(probationTeam(), LocalDateTime.now()) isBool false
    }

    @Test
    fun `should be available for active shared room attribute`() {
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
        locationUsage = LocationUsage.SHARED,
        createdBy = "TEST",
      )

      roomAttributes.isAvailableFor(probationTeam(), LocalDateTime.now()) isBool true
    }
  }

  @Nested
  inner class Court {
    @Test
    fun `should be available for active court room attribute`() {
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

      roomAttributes.isAvailableFor(court(), LocalDateTime.now()) isBool true
    }

    @Test
    fun `should not be available for inactive court room attribute`() {
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
        locationStatus = LocationStatus.INACTIVE,
        locationUsage = LocationUsage.COURT,
        createdBy = "TEST",
      )

      roomAttributes.isAvailableFor(court(), LocalDateTime.now()) isBool false
    }

    @Test
    fun `should not be available for active probation room attribute`() {
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
        locationUsage = LocationUsage.PROBATION,
        createdBy = "TEST",
      )

      roomAttributes.isAvailableFor(court(), LocalDateTime.now()) isBool false
    }

    @Test
    fun `should be available for specific active court room attribute`() {
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
        locationUsage = LocationUsage.PROBATION,
        createdBy = "TEST",
        allowedParties = "COURT",
      )

      roomAttributes.isAvailableFor(probationTeam(code = "COURT"), LocalDateTime.now()) isBool true
    }

    @Test
    fun `should not be available for specific active court room attribute`() {
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
        locationUsage = LocationUsage.PROBATION,
        createdBy = "TEST",
        allowedParties = "COURT",
      )

      roomAttributes.isAvailableFor(probationTeam(code = "DIFFERENT_COURT"), LocalDateTime.now()) isBool false
    }

    @Test
    fun `should be available for active shared room attribute`() {
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
        locationUsage = LocationUsage.SHARED,
        createdBy = "TEST",
      )

      roomAttributes.isAvailableFor(court(), LocalDateTime.now()) isBool true
    }
  }
}

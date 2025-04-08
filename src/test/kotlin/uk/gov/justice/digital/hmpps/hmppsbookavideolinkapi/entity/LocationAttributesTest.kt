package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvillePrison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthPrison
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class LocationAttributesTest {
  @Test
  fun `should accept location attributes with a schedule if the location usage is not SCHEDULE`() {
    val roomAttributes = LocationAttribute.decoratedRoom(
      dpsLocationId = UUID.randomUUID(),
      prison = pentonvillePrison,
      locationStatus = LocationStatus.ACTIVE,
      locationUsage = LocationUsage.COURT,
      allowedParties = emptySet(),
      prisonVideoUrl = null,
      notes = null,
      createdBy = COURT_USER,
    )

    assertDoesNotThrow {
      LocationSchedule.newSchedule(
        startDayOfWeek = DayOfWeek.MONDAY.value,
        endDayOfWeek = DayOfWeek.SUNDAY.value,
        startTime = LocalTime.of(1, 0),
        endTime = LocalTime.of(23, 0),
        locationUsage = LocationScheduleUsage.COURT,
        allowedParties = emptySet(),
        createdBy = COURT_USER,
        locationAttribute = roomAttributes,
      )
    }
  }

  @Test
  fun `should accept attributes with a schedule if location usage is SCHEDULE`() {
    val roomAttributes = LocationAttribute.decoratedRoom(
      dpsLocationId = UUID.randomUUID(),
      prison = pentonvillePrison,
      locationStatus = LocationStatus.ACTIVE,
      locationUsage = LocationUsage.SCHEDULE,
      allowedParties = emptySet(),
      prisonVideoUrl = null,
      notes = null,
      createdBy = COURT_USER,
    )

    roomAttributes.apply {
      addSchedule(
        usage = LocationScheduleUsage.COURT,
        startDayOfWeek = DayOfWeek.MONDAY.value,
        endDayOfWeek = DayOfWeek.SUNDAY.value,
        startTime = LocalTime.of(1, 0),
        endTime = LocalTime.of(23, 0),
        allowedParties = emptySet(),
        createdBy = COURT_USER,
      )

      addSchedule(
        usage = LocationScheduleUsage.PROBATION,
        startDayOfWeek = DayOfWeek.MONDAY.value,
        endDayOfWeek = DayOfWeek.SUNDAY.value,
        startTime = LocalTime.of(14, 0),
        endTime = LocalTime.of(15, 0),
        allowedParties = setOf("C", "A", "B"),
        createdBy = COURT_USER,
      )
    }

    with(roomAttributes.schedule()[0]) {
      locationUsage isEqualTo LocationScheduleUsage.COURT
      startDayOfWeek isEqualTo 1
      endDayOfWeek isEqualTo 7
      startTime isEqualTo LocalTime.of(1, 0)
      endTime isEqualTo LocalTime.of(23, 0)
      allowedParties isEqualTo null
      createdBy isEqualTo COURT_USER.username
      createdTime isCloseTo LocalDateTime.now()
    }

    with(roomAttributes.schedule()[1]) {
      locationUsage isEqualTo LocationScheduleUsage.PROBATION
      startDayOfWeek isEqualTo 1
      endDayOfWeek isEqualTo 7
      startTime isEqualTo LocalTime.of(14, 0)
      endTime isEqualTo LocalTime.of(15, 0)
      allowedParties isEqualTo "A,B,C"
      createdBy isEqualTo COURT_USER.username
      createdTime isCloseTo LocalDateTime.now()
    }
  }

  @Test
  fun `should reject a duplicate schedule row`() {
    val roomAttributes = LocationAttribute.decoratedRoom(
      dpsLocationId = UUID.randomUUID(),
      prison = pentonvillePrison,
      locationStatus = LocationStatus.ACTIVE,
      locationUsage = LocationUsage.SCHEDULE,
      allowedParties = emptySet(),
      prisonVideoUrl = null,
      notes = null,
      createdBy = COURT_USER,
    )

    roomAttributes.addSchedule(
      usage = LocationScheduleUsage.COURT,
      startDayOfWeek = DayOfWeek.MONDAY.value,
      endDayOfWeek = DayOfWeek.SUNDAY.value,
      startTime = LocalTime.of(1, 0),
      endTime = LocalTime.of(23, 0),
      allowedParties = emptySet(),
      createdBy = COURT_USER,
    )

    assertThrows<IllegalArgumentException> {
      roomAttributes.addSchedule(
        usage = LocationScheduleUsage.COURT,
        startDayOfWeek = DayOfWeek.MONDAY.value,
        endDayOfWeek = DayOfWeek.SUNDAY.value,
        startTime = LocalTime.of(1, 0),
        endTime = LocalTime.of(23, 0),
        allowedParties = emptySet(),
        createdBy = COURT_USER,
      )
    }.message isEqualTo "Cannot add a duplicate schedule row to location attribute with ID 0."
  }

  @Test
  fun `should amend a room attribute`() {
    val roomAttributes = LocationAttribute.decoratedRoom(
      dpsLocationId = wandsworthLocation.id,
      prison = wandsworthPrison,
      locationUsage = LocationUsage.SHARED,
      locationStatus = LocationStatus.INACTIVE,
      allowedParties = emptySet(),
      prisonVideoUrl = null,
      notes = null,
      createdBy = COURT_USER,
    )

    with(roomAttributes) {
      locationUsage isEqualTo LocationUsage.SHARED
      locationStatus isEqualTo LocationStatus.INACTIVE
      allowedParties isEqualTo null
      prisonVideoUrl isEqualTo null
      notes isEqualTo null
      amendedBy isEqualTo null
      amendedTime isEqualTo null
    }

    roomAttributes.amend(
      locationUsage = LocationUsage.PROBATION,
      locationStatus = LocationStatus.ACTIVE,
      allowedParties = setOf("PROBATION"),
      prisonVideoUrl = "prison-room-url",
      comments = "updated notes",
      amendedBy = PROBATION_USER,
    )

    with(roomAttributes) {
      locationUsage isEqualTo LocationUsage.PROBATION
      locationStatus isEqualTo LocationStatus.ACTIVE
      allowedParties isEqualTo "PROBATION"
      prisonVideoUrl isEqualTo "prison-room-url"
      notes isEqualTo "updated notes"
      amendedBy isEqualTo PROBATION_USER.username
      amendedTime isCloseTo LocalDateTime.now()
    }
  }

  @Nested
  inner class Probation {
    @Test
    fun `should be PROBATION ANY for active probation room attribute`() {
      val roomAttributes = LocationAttribute.decoratedRoom(
        dpsLocationId = UUID.randomUUID(),
        prison = pentonvillePrison,
        locationUsage = LocationUsage.PROBATION,
        locationStatus = LocationStatus.ACTIVE,
        allowedParties = emptySet(),
        prisonVideoUrl = null,
        notes = null,
        createdBy = PROBATION_USER,
      )

      roomAttributes.isAvailableFor(probationTeam(), today().atTime(12, 0)) isEqualTo AvailabilityStatus.PROBATION_ANY
    }

    @Test
    fun `should be NONE for inactive probation room attribute`() {
      val roomAttributes = LocationAttribute.decoratedRoom(
        dpsLocationId = UUID.randomUUID(),
        prison = pentonvillePrison,
        locationUsage = LocationUsage.PROBATION,
        locationStatus = LocationStatus.INACTIVE,
        allowedParties = emptySet(),
        prisonVideoUrl = null,
        notes = null,
        createdBy = PROBATION_USER,
      )

      roomAttributes.isAvailableFor(probationTeam(), today().atTime(12, 0)) isEqualTo AvailabilityStatus.NONE
    }

    @Test
    fun `should be PROBATION_TEAM for specific active probation room attribute`() {
      val roomAttributes = LocationAttribute.decoratedRoom(
        dpsLocationId = UUID.randomUUID(),
        prison = pentonvillePrison,
        locationStatus = LocationStatus.ACTIVE,
        locationUsage = LocationUsage.PROBATION,
        createdBy = PROBATION_USER,
        prisonVideoUrl = null,
        notes = null,
        allowedParties = setOf("TEAM_CODE"),
      )

      roomAttributes.isAvailableFor(
        probationTeam(code = "TEAM_CODE"),
        LocalDateTime.now(),
      ) isEqualTo AvailabilityStatus.PROBATION_ROOM
    }

    @Test
    fun `should be NONE for specific active probation room attribute`() {
      val roomAttributes = LocationAttribute.decoratedRoom(
        dpsLocationId = UUID.randomUUID(),
        prison = pentonvillePrison,
        locationStatus = LocationStatus.ACTIVE,
        locationUsage = LocationUsage.PROBATION,
        createdBy = PROBATION_USER,
        prisonVideoUrl = null,
        notes = null,
        allowedParties = setOf("TEAM_CODE"),
      )

      roomAttributes.isAvailableFor(
        probationTeam(code = "DIFFERENT_TEAM_CODE"),
        today().atTime(12, 0),
      ) isEqualTo AvailabilityStatus.NONE
    }

    @Test
    fun `should be NONE for active court room attribute`() {
      val roomAttributes = LocationAttribute.decoratedRoom(
        dpsLocationId = UUID.randomUUID(),
        prison = pentonvillePrison,
        locationStatus = LocationStatus.ACTIVE,
        locationUsage = LocationUsage.COURT,
        allowedParties = emptySet(),
        prisonVideoUrl = null,
        notes = null,
        createdBy = COURT_USER,
      )

      roomAttributes.isAvailableFor(probationTeam(), today().atTime(12, 0)) isEqualTo AvailabilityStatus.NONE
    }

    @Test
    fun `should be NONE for a probation team when schedule also blocked`() {
      val roomAttributes = LocationAttribute.decoratedRoom(
        dpsLocationId = UUID.randomUUID(),
        prison = pentonvillePrison,
        locationStatus = LocationStatus.ACTIVE,
        locationUsage = LocationUsage.SCHEDULE,
        allowedParties = emptySet(),
        prisonVideoUrl = null,
        notes = null,
        createdBy = PROBATION_USER,
      ).apply {
        schedule(this, locationUsage = LocationScheduleUsage.PROBATION)
        schedule(this, locationUsage = LocationScheduleUsage.BLOCKED)
      }

      roomAttributes.isAvailableFor(probationTeam(), today().atTime(12, 0)) isEqualTo AvailabilityStatus.NONE
    }

    @Test
    fun `should be SHARED for active shared room attribute`() {
      val roomAttributes = LocationAttribute.decoratedRoom(
        dpsLocationId = UUID.randomUUID(),
        prison = pentonvillePrison,
        locationStatus = LocationStatus.ACTIVE,
        locationUsage = LocationUsage.SHARED,
        allowedParties = emptySet(),
        prisonVideoUrl = null,
        notes = null,
        createdBy = PROBATION_USER,
      )

      roomAttributes.isAvailableFor(probationTeam(), today().atTime(12, 0)) isEqualTo AvailabilityStatus.SHARED
    }

    @Test
    fun `should be SHARED if the schedule is empty`() {
      val roomAttributes = LocationAttribute.decoratedRoom(
        dpsLocationId = UUID.randomUUID(),
        prison = pentonvillePrison,
        locationStatus = LocationStatus.ACTIVE,
        locationUsage = LocationUsage.SCHEDULE,
        allowedParties = emptySet(),
        prisonVideoUrl = null,
        notes = null,
        createdBy = PROBATION_USER,
      )

      roomAttributes.isAvailableFor(probationTeam(), today().atTime(12, 0)) isEqualTo AvailabilityStatus.SHARED
    }

    @Test
    fun `should be PROBATION ANY for any probation schedule`() {
      val roomAttributes = LocationAttribute.decoratedRoom(
        dpsLocationId = UUID.randomUUID(),
        prison = pentonvillePrison,
        locationStatus = LocationStatus.ACTIVE,
        locationUsage = LocationUsage.SCHEDULE,
        allowedParties = emptySet(),
        prisonVideoUrl = null,
        notes = null,
        createdBy = PROBATION_USER,
      ).apply { schedule(this, locationUsage = LocationScheduleUsage.PROBATION) }

      roomAttributes.isAvailableFor(probationTeam(), today().atTime(12, 0)) isEqualTo AvailabilityStatus.PROBATION_ANY
    }

    @Test
    fun `should be PROBATION_TEAM for probation team schedule`() {
      val roomAttributes = LocationAttribute.decoratedRoom(
        dpsLocationId = UUID.randomUUID(),
        prison = pentonvillePrison,
        locationStatus = LocationStatus.ACTIVE,
        locationUsage = LocationUsage.SCHEDULE,
        allowedParties = emptySet(),
        prisonVideoUrl = null,
        notes = null,
        createdBy = PROBATION_USER,
      ).apply {
        schedule(this, locationUsage = LocationScheduleUsage.PROBATION, allowedParties = setOf("PROBATION_TEAM"))
      }

      roomAttributes.isAvailableFor(probationTeam(code = "PROBATION_TEAM"), today().atTime(12, 0)) isEqualTo AvailabilityStatus.PROBATION_ROOM
    }

    @Test
    fun `should be NONE for court schedule`() {
      val roomAttributes = LocationAttribute.decoratedRoom(
        dpsLocationId = UUID.randomUUID(),
        prison = pentonvillePrison,
        locationStatus = LocationStatus.ACTIVE,
        locationUsage = LocationUsage.SCHEDULE,
        allowedParties = emptySet(),
        prisonVideoUrl = null,
        notes = null,
        createdBy = PROBATION_USER,
      ).apply { schedule(this, locationUsage = LocationScheduleUsage.COURT) }

      roomAttributes.isAvailableFor(probationTeam(), today().atTime(12, 0)) isEqualTo AvailabilityStatus.NONE
    }

    @Test
    fun `should be PROBATION ANY when schedule is blocked and probation but start time is on the boundary`() {
      val roomAttributes = LocationAttribute.decoratedRoom(
        dpsLocationId = UUID.randomUUID(),
        prison = pentonvillePrison,
        locationStatus = LocationStatus.ACTIVE,
        locationUsage = LocationUsage.SCHEDULE,
        allowedParties = emptySet(),
        prisonVideoUrl = null,
        notes = null,
        createdBy = PROBATION_USER,
      ).apply {
        schedule(this, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(10, 0), locationUsage = LocationScheduleUsage.BLOCKED)
        schedule(this, startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 0), locationUsage = LocationScheduleUsage.PROBATION)
      }

      roomAttributes.isAvailableFor(probationTeam(), today().atTime(10, 0)) isEqualTo AvailabilityStatus.PROBATION_ANY
    }
  }

  @Nested
  inner class Court {
    @Test
    fun `should be COURT ANY for active court room attribute`() {
      val roomAttributes = LocationAttribute.decoratedRoom(
        dpsLocationId = UUID.randomUUID(),
        prison = pentonvillePrison,
        locationUsage = LocationUsage.COURT,
        locationStatus = LocationStatus.ACTIVE,
        allowedParties = emptySet(),
        prisonVideoUrl = null,
        notes = null,
        createdBy = COURT_USER,
      )

      roomAttributes.isAvailableFor(court(), LocalDateTime.now()) isEqualTo AvailabilityStatus.COURT_ANY
    }

    @Test
    fun `should be COURT ANY for active court room attribute with probation only schedule`() {
      val roomAttributes = LocationAttribute.decoratedRoom(
        dpsLocationId = UUID.randomUUID(),
        prison = pentonvillePrison,
        locationUsage = LocationUsage.COURT,
        locationStatus = LocationStatus.ACTIVE,
        allowedParties = emptySet(),
        prisonVideoUrl = null,
        notes = null,
        createdBy = COURT_USER,
      ).apply {
        addSchedule(
          LocationScheduleUsage.PROBATION,
          startDayOfWeek = 1,
          endDayOfWeek = 7,
          startTime = LocalTime.of(8, 0),
          endTime = LocalTime.of(18, 0),
          allowedParties = emptySet(),
          notes = null,
          createdBy = COURT_USER,
        )
      }

      roomAttributes.isAvailableFor(court(), LocalDate.now().atTime(12, 0)) isEqualTo AvailabilityStatus.COURT_ANY
    }

    @Test
    fun `should be NONE for inactive court room attribute`() {
      val roomAttributes = LocationAttribute.decoratedRoom(
        dpsLocationId = UUID.randomUUID(),
        prison = pentonvillePrison,
        locationUsage = LocationUsage.COURT,
        locationStatus = LocationStatus.INACTIVE,
        allowedParties = emptySet(),
        prisonVideoUrl = null,
        notes = null,
        createdBy = COURT_USER,
      )

      roomAttributes.isAvailableFor(court(), LocalDateTime.now()) isEqualTo AvailabilityStatus.NONE
    }

    @Test
    fun `should be NONE for active probation room attribute`() {
      val roomAttributes = LocationAttribute.decoratedRoom(
        dpsLocationId = UUID.randomUUID(),
        prison = pentonvillePrison,
        locationUsage = LocationUsage.PROBATION,
        locationStatus = LocationStatus.ACTIVE,
        allowedParties = emptySet(),
        prisonVideoUrl = null,
        notes = null,
        createdBy = PROBATION_USER,
      )

      roomAttributes.isAvailableFor(court(), LocalDateTime.now()) isEqualTo AvailabilityStatus.NONE
    }

    @Test
    fun `should be COURT ROOM for specific active court room attribute`() {
      val roomAttributes = LocationAttribute.decoratedRoom(
        dpsLocationId = UUID.randomUUID(),
        prison = pentonvillePrison,
        locationUsage = LocationUsage.COURT,
        locationStatus = LocationStatus.ACTIVE,
        createdBy = COURT_USER,
        prisonVideoUrl = null,
        notes = null,
        allowedParties = setOf("COURT"),
      )

      roomAttributes.isAvailableFor(court(code = "COURT"), LocalDateTime.now()) isEqualTo AvailabilityStatus.COURT_ROOM
    }

    @Test
    fun `should be NONE for specific active court room attribute`() {
      val roomAttributes = LocationAttribute.decoratedRoom(
        dpsLocationId = UUID.randomUUID(),
        prison = pentonvillePrison,
        locationStatus = LocationStatus.ACTIVE,
        locationUsage = LocationUsage.COURT,
        createdBy = COURT_USER,
        prisonVideoUrl = null,
        notes = null,
        allowedParties = setOf("COURT"),
      )

      roomAttributes.isAvailableFor(court(code = "DIFFERENT_COURT"), LocalDateTime.now()) isEqualTo AvailabilityStatus.NONE
    }

    @Test
    fun `should be SHARED for active shared room attribute`() {
      val roomAttributes = LocationAttribute.decoratedRoom(
        dpsLocationId = UUID.randomUUID(),
        prison = pentonvillePrison,
        locationStatus = LocationStatus.ACTIVE,
        locationUsage = LocationUsage.SHARED,
        allowedParties = emptySet(),
        prisonVideoUrl = null,
        notes = null,
        createdBy = COURT_USER,
      )

      roomAttributes.isAvailableFor(court(), LocalDateTime.now()) isEqualTo AvailabilityStatus.SHARED
    }

    @Test
    fun `should be SHARED if the schedule is empty`() {
      val roomAttributes = LocationAttribute.decoratedRoom(
        dpsLocationId = UUID.randomUUID(),
        prison = pentonvillePrison,
        locationStatus = LocationStatus.ACTIVE,
        locationUsage = LocationUsage.SCHEDULE,
        allowedParties = emptySet(),
        prisonVideoUrl = null,
        notes = null,
        createdBy = COURT_USER,
      )

      roomAttributes.isAvailableFor(court(), today().atTime(12, 0)) isEqualTo AvailabilityStatus.SHARED
    }

    @Test
    fun `should be PROBATION ANY for any probation schedule`() {
      val roomAttributes = LocationAttribute.decoratedRoom(
        dpsLocationId = UUID.randomUUID(),
        prison = pentonvillePrison,
        locationStatus = LocationStatus.ACTIVE,
        locationUsage = LocationUsage.SCHEDULE,
        allowedParties = emptySet(),
        prisonVideoUrl = null,
        notes = null,
        createdBy = COURT_USER,
      ).apply { schedule(this, locationUsage = LocationScheduleUsage.COURT) }

      roomAttributes.isAvailableFor(court(), today().atTime(12, 0)) isEqualTo AvailabilityStatus.COURT_ANY
    }

    @Test
    fun `should be PROBATION_TEAM for probation team schedule`() {
      val roomAttributes = LocationAttribute.decoratedRoom(
        dpsLocationId = UUID.randomUUID(),
        prison = pentonvillePrison,
        locationStatus = LocationStatus.ACTIVE,
        locationUsage = LocationUsage.SCHEDULE,
        allowedParties = emptySet(),
        prisonVideoUrl = null,
        notes = null,
        createdBy = COURT_USER,
      ).apply {
        schedule(this, locationUsage = LocationScheduleUsage.COURT, allowedParties = setOf("COURT"))
      }

      roomAttributes.isAvailableFor(court(code = "COURT"), today().atTime(12, 0)) isEqualTo AvailabilityStatus.COURT_ROOM
    }

    @Test
    fun `should be NONE for a court when schedule also blocked`() {
      val roomAttributes = LocationAttribute.decoratedRoom(
        dpsLocationId = UUID.randomUUID(),
        prison = pentonvillePrison,
        locationStatus = LocationStatus.ACTIVE,
        locationUsage = LocationUsage.SCHEDULE,
        allowedParties = emptySet(),
        prisonVideoUrl = null,
        notes = null,
        createdBy = COURT_USER,
      ).apply {
        schedule(this, locationUsage = LocationScheduleUsage.COURT)
        schedule(this, locationUsage = LocationScheduleUsage.BLOCKED)
      }

      roomAttributes.isAvailableFor(court(), today().atTime(12, 0)) isEqualTo AvailabilityStatus.NONE
    }

    @Test
    fun `should be NONE for court schedule`() {
      val roomAttributes = LocationAttribute.decoratedRoom(
        dpsLocationId = UUID.randomUUID(),
        prison = pentonvillePrison,
        locationStatus = LocationStatus.ACTIVE,
        locationUsage = LocationUsage.SCHEDULE,
        allowedParties = emptySet(),
        prisonVideoUrl = null,
        notes = null,
        createdBy = COURT_USER,
      ).apply { schedule(this, locationUsage = LocationScheduleUsage.PROBATION) }

      roomAttributes.isAvailableFor(court(), today().atTime(12, 0)) isEqualTo AvailabilityStatus.NONE
    }

    @Test
    fun `should be COURT ANY when schedule is blocked and court but start time in on the boundary`() {
      val roomAttributes = LocationAttribute.decoratedRoom(
        dpsLocationId = UUID.randomUUID(),
        prison = pentonvillePrison,
        locationStatus = LocationStatus.ACTIVE,
        locationUsage = LocationUsage.SCHEDULE,
        allowedParties = emptySet(),
        prisonVideoUrl = null,
        notes = null,
        createdBy = COURT_USER,
      ).apply {
        schedule(this, startTime = LocalTime.of(9, 0), endTime = LocalTime.of(10, 0), locationUsage = LocationScheduleUsage.BLOCKED)
        schedule(this, startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 0), locationUsage = LocationScheduleUsage.COURT)
      }

      roomAttributes.isAvailableFor(court(), today().atTime(10, 0)) isEqualTo AvailabilityStatus.COURT_ANY
    }
  }

  private fun schedule(
    locationAttribute: LocationAttribute,
    start: DayOfWeek = DayOfWeek.MONDAY,
    end: DayOfWeek = DayOfWeek.SUNDAY,
    startTime: LocalTime = LocalTime.of(9, 0),
    endTime: LocalTime = LocalTime.of(17, 0),
    locationUsage: LocationScheduleUsage,
    allowedParties: Set<String> = emptySet(),
  ) {
    locationAttribute.addSchedule(
      usage = locationUsage,
      startDayOfWeek = start.value,
      endDayOfWeek = end.value,
      startTime = startTime,
      endTime = endTime,
      createdBy = COURT_USER,
      allowedParties = allowedParties,
    )
  }
}

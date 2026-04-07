package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.availability

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.AvailabilityStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactly
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation2
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation3
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AvailableLocation
import java.time.LocalTime

class TimeSlotLocationsBuilderTest {
  @Nested
  inner class AvailableProbationLocations {
    private val locationOne10am11am = AvailableLocation(
      name = wandsworthLocation.key,
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      dpsLocationKey = wandsworthLocation.key,
      dpsLocationId = wandsworthLocation.id,
      usage = LocationUsage.PROBATION,
      timeSlot = TimeSlot.AM,
    )
    private val locationTwo10am11am = AvailableLocation(
      name = wandsworthLocation2.key,
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      dpsLocationKey = wandsworthLocation2.key,
      dpsLocationId = wandsworthLocation2.id,
      usage = LocationUsage.PROBATION,
      timeSlot = TimeSlot.AM,
    )
    private val locationThree11am12am = AvailableLocation(
      name = wandsworthLocation3.key,
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(12, 0),
      dpsLocationKey = wandsworthLocation3.key,
      dpsLocationId = wandsworthLocation3.id,
      usage = LocationUsage.SHARED,
      timeSlot = TimeSlot.AM,
    )

    @Test
    fun `should favour PROBATION_ROOM over PROBATION_ANY when times match`() {
      val availableLocations = TimeSlotLocationsBuilder.builder {
        add(availabilityStatus = AvailabilityStatus.PROBATION_ROOM, availableLocation = locationOne10am11am)
        add(availabilityStatus = AvailabilityStatus.PROBATION_ANY, availableLocation = locationTwo10am11am)
        add(availabilityStatus = AvailabilityStatus.SHARED, availableLocation = locationThree11am12am)
      }.build()

      availableLocations containsExactly listOf(locationOne10am11am, locationThree11am12am)
    }

    @Test
    fun `should favour PROBATION_ANY over SHARED when times match`() {
      val availableLocations = TimeSlotLocationsBuilder.builder {
        add(availabilityStatus = AvailabilityStatus.PROBATION_ANY, availableLocation = locationOne10am11am)
        add(availabilityStatus = AvailabilityStatus.SHARED, availableLocation = locationTwo10am11am)
        add(availabilityStatus = AvailabilityStatus.SHARED, availableLocation = locationThree11am12am)
      }.build()

      availableLocations containsExactly listOf(locationOne10am11am, locationThree11am12am)
    }
  }

  @Nested
  inner class AvailableScheduleProbationLocations {
    private val locationOne10am11am = AvailableLocation(
      name = wandsworthLocation.key,
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      dpsLocationKey = wandsworthLocation.key,
      dpsLocationId = wandsworthLocation.id,
      usage = LocationUsage.SCHEDULE,
      timeSlot = TimeSlot.AM,
    )
    private val locationTwo10am11am = AvailableLocation(
      name = wandsworthLocation2.key,
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      dpsLocationKey = wandsworthLocation2.key,
      dpsLocationId = wandsworthLocation2.id,
      usage = LocationUsage.SCHEDULE,
      timeSlot = TimeSlot.AM,
    )
    private val locationThree11am12am = AvailableLocation(
      name = wandsworthLocation3.key,
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(12, 0),
      dpsLocationKey = wandsworthLocation3.key,
      dpsLocationId = wandsworthLocation3.id,
      usage = LocationUsage.SHARED,
      timeSlot = TimeSlot.AM,
    )

    @Test
    fun `should favour PROBATION_ROOM over PROBATION_COURT when times match`() {
      val availableLocations = TimeSlotLocationsBuilder.builder {
        add(availabilityStatus = AvailabilityStatus.PROBATION_ROOM, availableLocation = locationOne10am11am)
        add(availabilityStatus = AvailabilityStatus.PROBATION_COURT, availableLocation = locationTwo10am11am)
        add(availabilityStatus = AvailabilityStatus.SHARED, availableLocation = locationThree11am12am)
      }.build()

      availableLocations containsExactly listOf(locationOne10am11am, locationThree11am12am)
    }

    @Test
    fun `should favour PROBATION_ROOM over PROBATION_SENTENCE when times match`() {
      val availableLocations = TimeSlotLocationsBuilder.builder {
        add(availabilityStatus = AvailabilityStatus.PROBATION_ROOM, availableLocation = locationOne10am11am)
        add(availabilityStatus = AvailabilityStatus.PROBATION_SENTENCE, availableLocation = locationTwo10am11am)
        add(availabilityStatus = AvailabilityStatus.SHARED, availableLocation = locationThree11am12am)
      }.build()

      availableLocations containsExactly listOf(locationOne10am11am, locationThree11am12am)
    }

    @Test
    fun `should favour PROBATION_ROOM over PROBATION_ANY when times match`() {
      val availableLocations = TimeSlotLocationsBuilder.builder {
        add(availabilityStatus = AvailabilityStatus.PROBATION_ROOM, availableLocation = locationOne10am11am)
        add(availabilityStatus = AvailabilityStatus.PROBATION_ANY, availableLocation = locationTwo10am11am)
        add(availabilityStatus = AvailabilityStatus.SHARED, availableLocation = locationThree11am12am)
      }.build()

      availableLocations containsExactly listOf(locationOne10am11am, locationThree11am12am)
    }

    @Test
    fun `should favour PROBATION_ROOM over SHARED when times match`() {
      val availableLocations = TimeSlotLocationsBuilder.builder {
        add(availabilityStatus = AvailabilityStatus.PROBATION_ROOM, availableLocation = locationOne10am11am)
        add(availabilityStatus = AvailabilityStatus.SHARED, availableLocation = locationTwo10am11am)
        add(availabilityStatus = AvailabilityStatus.SHARED, availableLocation = locationThree11am12am)
      }.build()

      availableLocations containsExactly listOf(locationOne10am11am, locationThree11am12am)
    }

    @Test
    fun `should favour PROBATION_COURT over PROBATION_ANY when times match`() {
      val availableLocations = TimeSlotLocationsBuilder.builder {
        add(availabilityStatus = AvailabilityStatus.PROBATION_COURT, availableLocation = locationOne10am11am)
        add(availabilityStatus = AvailabilityStatus.PROBATION_ANY, availableLocation = locationTwo10am11am)
        add(availabilityStatus = AvailabilityStatus.SHARED, availableLocation = locationThree11am12am)
      }.build()

      availableLocations containsExactly listOf(locationOne10am11am, locationThree11am12am)
    }

    @Test
    fun `should favour PROBATION_COURT over SHARED when times match`() {
      val availableLocations = TimeSlotLocationsBuilder.builder {
        add(availabilityStatus = AvailabilityStatus.PROBATION_COURT, availableLocation = locationOne10am11am)
        add(availabilityStatus = AvailabilityStatus.SHARED, availableLocation = locationTwo10am11am)
        add(availabilityStatus = AvailabilityStatus.SHARED, availableLocation = locationThree11am12am)
      }.build()

      availableLocations containsExactly listOf(locationOne10am11am, locationThree11am12am)
    }

    @Test
    fun `should favour PROBATION_SENTENCE over PROBATION_ANY when times match`() {
      val availableLocations = TimeSlotLocationsBuilder.builder {
        add(availabilityStatus = AvailabilityStatus.PROBATION_SENTENCE, availableLocation = locationOne10am11am)
        add(availabilityStatus = AvailabilityStatus.PROBATION_ANY, availableLocation = locationTwo10am11am)
        add(availabilityStatus = AvailabilityStatus.SHARED, availableLocation = locationThree11am12am)
      }.build()

      availableLocations containsExactly listOf(locationOne10am11am, locationThree11am12am)
    }

    @Test
    fun `should favour PROBATION_SENTENCE over SHARED when times match`() {
      val availableLocations = TimeSlotLocationsBuilder.builder {
        add(availabilityStatus = AvailabilityStatus.PROBATION_SENTENCE, availableLocation = locationOne10am11am)
        add(availabilityStatus = AvailabilityStatus.SHARED, availableLocation = locationTwo10am11am)
        add(availabilityStatus = AvailabilityStatus.SHARED, availableLocation = locationThree11am12am)
      }.build()

      availableLocations containsExactly listOf(locationOne10am11am, locationThree11am12am)
    }

    @Test
    fun `should favour PROBATION_ANY over SHARED when times match`() {
      val availableLocations = TimeSlotLocationsBuilder.builder {
        add(availabilityStatus = AvailabilityStatus.PROBATION_ANY, availableLocation = locationOne10am11am)
        add(availabilityStatus = AvailabilityStatus.SHARED, availableLocation = locationTwo10am11am)
        add(availabilityStatus = AvailabilityStatus.SHARED, availableLocation = locationThree11am12am)
      }.build()

      availableLocations containsExactly listOf(locationOne10am11am, locationThree11am12am)
    }
  }

  @Nested
  inner class AvailableCourtLocations {
    private val locationOne10am11am = AvailableLocation(
      name = wandsworthLocation.key,
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      dpsLocationKey = wandsworthLocation.key,
      dpsLocationId = wandsworthLocation.id,
      usage = LocationUsage.COURT,
      timeSlot = TimeSlot.AM,
    )
    private val locationTwo10am11am = AvailableLocation(
      name = wandsworthLocation2.key,
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      dpsLocationKey = wandsworthLocation2.key,
      dpsLocationId = wandsworthLocation2.id,
      usage = LocationUsage.COURT,
      timeSlot = TimeSlot.AM,
    )
    private val locationThree11am12am = AvailableLocation(
      name = wandsworthLocation3.key,
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(12, 0),
      dpsLocationKey = wandsworthLocation3.key,
      dpsLocationId = wandsworthLocation3.id,
      usage = LocationUsage.SHARED,
      timeSlot = TimeSlot.AM,
    )

    @Test
    fun `should favour COURT_ROOM over COURT_ANY when times match`() {
      val availableLocations = TimeSlotLocationsBuilder.builder {
        add(availabilityStatus = AvailabilityStatus.COURT_ROOM, availableLocation = locationOne10am11am)
        add(availabilityStatus = AvailabilityStatus.COURT_ANY, availableLocation = locationTwo10am11am)
        add(availabilityStatus = AvailabilityStatus.SHARED, availableLocation = locationThree11am12am)
      }.build()

      availableLocations containsExactly listOf(locationOne10am11am, locationThree11am12am)
    }

    @Test
    fun `should favour COURT_ANY over SHARED when times match`() {
      val availableLocations = TimeSlotLocationsBuilder.builder {
        add(availabilityStatus = AvailabilityStatus.COURT_ANY, availableLocation = locationOne10am11am)
        add(availabilityStatus = AvailabilityStatus.SHARED, availableLocation = locationTwo10am11am)
        add(availabilityStatus = AvailabilityStatus.SHARED, availableLocation = locationThree11am12am)
      }.build()

      availableLocations containsExactly listOf(locationOne10am11am, locationThree11am12am)
    }
  }
}

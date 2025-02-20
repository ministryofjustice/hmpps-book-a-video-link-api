package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.PrisonRegime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BLACKPOOL_MC_PPOC
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation2
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AvailableLocationsRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AvailableLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import java.time.LocalTime

class AvailableLocationsServiceTest {
  private val locationsService: LocationsService = mock()
  private val bookedLocationsService: BookedLocationsService = mock()
  private val prisonRegime: PrisonRegime = mock()
  private val service = AvailableLocationsService(locationsService, bookedLocationsService, prisonRegime)

  @Test
  fun `should fail if capped number of available locations is not positive`() {
    assertThrows<IllegalArgumentException> { service.findAvailableLocations(mock(), 0) }
      .message isEqualTo "The cap for the maximum number of available slots must be a positive number"

    assertThrows<IllegalArgumentException> { service.findAvailableLocations(mock(), -1) }
      .message isEqualTo "The cap for the maximum number of available slots must be a positive number"
  }

  @DisplayName("Testing undecorated locations only")
  @Nested
  inner class UndecoratedLocations {

    private val location1 = wandsworthLocation.toModel()
    private val location2 = wandsworthLocation2.toModel()

    @BeforeEach
    fun before() {
      whenever(prisonRegime.startOfDay(WANDSWORTH)) doReturn LocalTime.of(9, 0)
      whenever(prisonRegime.endOfDay(WANDSWORTH)) doReturn LocalTime.of(17, 0)
    }

    @Test
    fun `should return 5 available morning times for location 1 with one blocked out booking`() {
      whenever(locationsService.getDecoratedVideoLocations(WANDSWORTH, true)) doReturn listOf(location1)
      whenever(bookedLocationsService.findBooked(WANDSWORTH, tomorrow(), listOf(location1))) doReturn BookedLocations(
        listOf(BookedLocation(location1, LocalTime.of(10, 0), LocalTime.of(11, 0))),
      )

      val response = service.findAvailableLocations(
        AvailableLocationsRequest(
          prisonCode = WANDSWORTH,
          bookingType = BookingType.PROBATION,
          probationTeamCode = BLACKPOOL_MC_PPOC,
          date = tomorrow(),
          bookingDuration = 60,
          timeSlots = listOf(TimeSlot.AM),
        ),
      )

      response.locations hasSize 5

      response.locations containsExactlyInAnyOrder listOf(
        availableLocation(location1, time(9, 0), time(10, 0)),
        availableLocation(location1, time(11, 0), time(12, 0)),
        availableLocation(location1, time(11, 15), time(12, 15)),
        availableLocation(location1, time(11, 30), time(12, 30)),
        availableLocation(location1, time(11, 45), time(12, 45)),
      )
    }

    @Test
    fun `should return 13 available morning times for location 1 and 2 with two blocked out bookings`() {
      whenever(locationsService.getDecoratedVideoLocations(WANDSWORTH, true)) doReturn listOf(location1, location2)
      whenever(
        bookedLocationsService.findBooked(
          WANDSWORTH,
          tomorrow(),
          listOf(location1, location2),
        ),
      ) doReturn BookedLocations(
        listOf(
          BookedLocation(location1, LocalTime.of(10, 0), LocalTime.of(11, 0)),
          BookedLocation(location2, LocalTime.of(9, 0), LocalTime.of(10, 0)),
        ),
      )

      val response = service.findAvailableLocations(
        AvailableLocationsRequest(
          prisonCode = WANDSWORTH,
          bookingType = BookingType.PROBATION,
          probationTeamCode = BLACKPOOL_MC_PPOC,
          date = tomorrow(),
          bookingDuration = 60,
          timeSlots = listOf(TimeSlot.AM),
        ),
        20,

      )

      response.locations hasSize 13

      response.locations containsExactlyInAnyOrder listOf(
        availableLocation(location1, time(9, 0), time(10, 0)),
        availableLocation(location1, time(11, 0), time(12, 0)),
        availableLocation(location1, time(11, 15), time(12, 15)),
        availableLocation(location1, time(11, 30), time(12, 30)),
        availableLocation(location1, time(11, 45), time(12, 45)),
        availableLocation(location2, time(10, 0), time(11, 0)),
        availableLocation(location2, time(10, 15), time(11, 15)),
        availableLocation(location2, time(10, 30), time(11, 30)),
        availableLocation(location2, time(10, 45), time(11, 45)),
        availableLocation(location2, time(11, 0), time(12, 0)),
        availableLocation(location2, time(11, 15), time(12, 15)),
        availableLocation(location2, time(11, 30), time(12, 30)),
        availableLocation(location2, time(11, 45), time(12, 45)),
      )
    }

    @Test
    fun `should return 20 available afternoon times for location 1 when on blocked out booking`() {
      whenever(locationsService.getDecoratedVideoLocations(WANDSWORTH, true)) doReturn listOf(location1)
      whenever(bookedLocationsService.findBooked(WANDSWORTH, tomorrow(), listOf(location1))) doReturn BookedLocations(
        listOf(BookedLocation(location1, LocalTime.of(10, 0), LocalTime.of(11, 0))),
      )

      val response = service.findAvailableLocations(
        AvailableLocationsRequest(
          prisonCode = WANDSWORTH,
          bookingType = BookingType.PROBATION,
          probationTeamCode = BLACKPOOL_MC_PPOC,
          date = tomorrow(),
          bookingDuration = 60,
          timeSlots = listOf(TimeSlot.PM),
        ),
        20,
      )

      response.locations hasSize 20

      response.locations containsExactlyInAnyOrder listOf(
        availableLocation(location1, time(12, 0), time(13, 0)),
        availableLocation(location1, time(12, 15), time(13, 15)),
        availableLocation(location1, time(12, 30), time(13, 30)),
        availableLocation(location1, time(12, 45), time(13, 45)),
        availableLocation(location1, time(13, 0), time(14, 0)),
        availableLocation(location1, time(13, 15), time(14, 15)),
        availableLocation(location1, time(13, 30), time(14, 30)),
        availableLocation(location1, time(13, 45), time(14, 45)),
        availableLocation(location1, time(14, 0), time(15, 0)),
        availableLocation(location1, time(14, 15), time(15, 15)),
        availableLocation(location1, time(14, 30), time(15, 30)),
        availableLocation(location1, time(14, 45), time(15, 45)),
        availableLocation(location1, time(15, 0), time(16, 0)),
        availableLocation(location1, time(15, 15), time(16, 15)),
        availableLocation(location1, time(15, 30), time(16, 30)),
        availableLocation(location1, time(15, 45), time(16, 45)),
        availableLocation(location1, time(16, 0), time(17, 0)),
        availableLocation(location1, time(16, 15), time(17, 15)),
        availableLocation(location1, time(16, 30), time(17, 30)),
        availableLocation(location1, time(16, 45), time(17, 45)),
      )
    }

    @Test
    fun `should cap to 10 available afternoon times for location 1 when on blocked out booking`() {
      whenever(locationsService.getDecoratedVideoLocations(WANDSWORTH, true)) doReturn listOf(location1)
      whenever(bookedLocationsService.findBooked(WANDSWORTH, tomorrow(), listOf(location1))) doReturn BookedLocations(
        listOf(BookedLocation(location1, LocalTime.of(10, 0), LocalTime.of(11, 0))),
      )

      val response = service.findAvailableLocations(
        AvailableLocationsRequest(
          prisonCode = WANDSWORTH,
          bookingType = BookingType.PROBATION,
          probationTeamCode = BLACKPOOL_MC_PPOC,
          date = tomorrow(),
          bookingDuration = 60,
          timeSlots = listOf(TimeSlot.PM),
        ),
        10,
      )

      response.locations hasSize 10

      response.locations containsExactlyInAnyOrder listOf(
        availableLocation(location1, time(12, 0), time(13, 0)),
        availableLocation(location1, time(12, 15), time(13, 15)),
        availableLocation(location1, time(12, 30), time(13, 30)),
        availableLocation(location1, time(12, 45), time(13, 45)),
        availableLocation(location1, time(13, 0), time(14, 0)),
        availableLocation(location1, time(13, 15), time(14, 15)),
        availableLocation(location1, time(13, 30), time(14, 30)),
        availableLocation(location1, time(13, 45), time(14, 45)),
        availableLocation(location1, time(14, 0), time(15, 0)),
        availableLocation(location1, time(14, 15), time(15, 15)),
      )
    }

    @Test
    fun `should return 18 available morning and afternoon times for location 1 when two block out bookings`() {
      whenever(locationsService.getDecoratedVideoLocations(WANDSWORTH, true)) doReturn listOf(location1)
      whenever(bookedLocationsService.findBooked(WANDSWORTH, tomorrow(), listOf(location1))) doReturn BookedLocations(
        listOf(
          BookedLocation(location1, LocalTime.of(10, 0), LocalTime.of(11, 0)),
          BookedLocation(location1, LocalTime.of(13, 0), LocalTime.of(14, 0)),
        ),
      )

      val response = service.findAvailableLocations(
        AvailableLocationsRequest(
          prisonCode = WANDSWORTH,
          bookingType = BookingType.PROBATION,
          probationTeamCode = BLACKPOOL_MC_PPOC,
          date = tomorrow(),
          bookingDuration = 60,
          timeSlots = listOf(TimeSlot.AM, TimeSlot.PM),
        ),
        18,
      )

      response.locations hasSize 18

      response.locations containsExactlyInAnyOrder listOf(
        availableLocation(location1, time(9, 0), time(10, 0)),
        availableLocation(location1, time(11, 0), time(12, 0)),
        availableLocation(location1, time(11, 15), time(12, 15)),
        availableLocation(location1, time(11, 30), time(12, 30)),
        availableLocation(location1, time(11, 45), time(12, 45)),
        availableLocation(location1, time(12, 0), time(13, 0)),
        availableLocation(location1, time(14, 0), time(15, 0)),
        availableLocation(location1, time(14, 15), time(15, 15)),
        availableLocation(location1, time(14, 30), time(15, 30)),
        availableLocation(location1, time(14, 45), time(15, 45)),
        availableLocation(location1, time(15, 0), time(16, 0)),
        availableLocation(location1, time(15, 15), time(16, 15)),
        availableLocation(location1, time(15, 30), time(16, 30)),
        availableLocation(location1, time(15, 45), time(16, 45)),
        availableLocation(location1, time(16, 0), time(17, 0)),
        availableLocation(location1, time(16, 15), time(17, 15)),
        availableLocation(location1, time(16, 30), time(17, 30)),
        availableLocation(location1, time(16, 45), time(17, 45)),
      )
    }
  }

  private fun time(hour: Int, minute: Int) = LocalTime.of(hour, minute)

  private fun availableLocation(location: Location, startTime: LocalTime, endTime: LocalTime) = AvailableLocation(
    name = location.description!!,
    startTime = startTime,
    endTime = endTime,
    dpsLocationKey = location.key,
    dpsLocationId = location.dpsLocationId,
    usage = null,
  )
}

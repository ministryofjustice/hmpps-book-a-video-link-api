package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.availability

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TimeSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.AvailabilityStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BLACKPOOL_MC_PPOC
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.RISLEY
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactly
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.risleyLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.risleyLocation2
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation2
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation3
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.yesterday
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.RoomAttributes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.DateTimeAvailabilityRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AvailableLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.slot
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional

class DateTimeAvailabilityServiceTest {
  private val locationsService: LocationsService = mock()
  private val bookedLocationsService: BookedLocationsService = mock()
  private val locationAttributesService: LocationAttributesAvailableService = mock()
  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()

  @Test
  fun `should fail when request date and start time is in the past`() {
    assertThrows<IllegalArgumentException> {
      service { LocalDate.now().atStartOfDay() }.findAvailable(
        DateTimeAvailabilityRequest(
          prisonCode = RISLEY,
          bookingType = BookingType.PROBATION,
          probationTeamCode = BLACKPOOL_MC_PPOC,
          date = yesterday(),
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(11, 0),
        ),
      )
    }.message isEqualTo "Requested date and start time must be in the future."
  }

  @DisplayName("Testing for available undecorated locations")
  @Nested
  inner class RequestsForUndecoratedRoomsOnly {

    private val location1 = risleyLocation.toModel()
    private val location2 = risleyLocation2.toModel()

    @Test
    fun `should return 2 available morning times for location 1 and 2 with time of request 10am`() {
      whenever(locationsService.getVideoLinkLocationsAtPrison(RISLEY, true)) doReturn listOf(location1, location2)
      whenever(bookedLocationsService.findBooked(BookedLookup(RISLEY, today(), listOf(location1, location2)))) doReturn BookedLocations(emptyList())

      val response = service { LocalDate.now().atStartOfDay() }.findAvailable(
        DateTimeAvailabilityRequest(
          prisonCode = RISLEY,
          bookingType = BookingType.PROBATION,
          probationTeamCode = BLACKPOOL_MC_PPOC,
          date = today(),
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(11, 0),
        ),
      )

      response.locations hasSize 2

      response.locations containsExactly listOf(
        availableLocation(location1, time(10, 0), time(11, 0), LocationUsage.SHARED),
        availableLocation(location2, time(10, 0), time(11, 0), LocationUsage.SHARED),
      )
    }

    @Test
    fun `should return 1 available morning time for location 1 only with time of request 10am`() {
      whenever(locationsService.getVideoLinkLocationsAtPrison(RISLEY, true)) doReturn listOf(location1, location2)
      whenever(bookedLocationsService.findBooked(BookedLookup(RISLEY, today(), listOf(location1, location2)))) doReturn BookedLocations(
        listOf(
          BookedLocation(
            location2,
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
          ),
        ),
      )

      val response = service { LocalDate.now().atStartOfDay() }.findAvailable(
        DateTimeAvailabilityRequest(
          prisonCode = RISLEY,
          bookingType = BookingType.PROBATION,
          probationTeamCode = BLACKPOOL_MC_PPOC,
          date = today(),
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(11, 0),
        ),
      )

      response.locations hasSize 1

      response.locations containsExactly listOf(
        availableLocation(location1, time(10, 0), time(11, 0), LocationUsage.SHARED),
      )
    }
  }

  @Nested
  inner class RequestsForUndecoratedAndDecoratedRooms {
    private val decoratedProbationLocation = wandsworthLocation.toModel().copy(
      description = "b - decorated probation room",
      extraAttributes = RoomAttributes(
        attributeId = 1,
        locationStatus = LocationStatus.ACTIVE,
        statusMessage = null,
        yesterday(),
        LocationUsage.PROBATION,
        allowedParties = emptyList(),
        prisonVideoUrl = null,
        notes = null,
      ),
    )
    private val decoratedProbationTeamLocation = wandsworthLocation2.toModel().copy(
      description = "c - decorated probation team room",
      extraAttributes = RoomAttributes(
        attributeId = 2,
        locationStatus = LocationStatus.ACTIVE,
        statusMessage = null,
        yesterday(),
        LocationUsage.PROBATION,
        allowedParties = listOf("BLACKPOOL_MC_PPOC"),
        prisonVideoUrl = null,
        notes = null,
      ),
    )
    private val undecoratedLocation = wandsworthLocation3.toModel().copy(description = "a - undecorated room")

    @Test
    fun `should include decorated probation room and undecorated`() {
      whenever(locationsService.getVideoLinkLocationsAtPrison(WANDSWORTH, true)) doReturn listOf(decoratedProbationLocation, undecoratedLocation)
      whenever(
        bookedLocationsService.findBooked(
          BookedLookup(
            WANDSWORTH,
            tomorrow(),
            listOf(decoratedProbationLocation, undecoratedLocation),
          ),
        ),
      ) doReturn BookedLocations(emptyList())

      whenever(locationAttributesService.isLocationAvailableFor(LocationAvailableRequest.probation(1, BLACKPOOL_MC_PPOC, tomorrow().atTime(9, 0)))) doReturn AvailabilityStatus.PROBATION_ANY

      val response = service().findAvailable(
        DateTimeAvailabilityRequest(
          prisonCode = WANDSWORTH,
          bookingType = BookingType.PROBATION,
          probationTeamCode = BLACKPOOL_MC_PPOC,
          date = tomorrow(),
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(10, 0),
        ),
      )

      response.locations hasSize 2

      response.locations containsExactly listOf(
        availableLocation(decoratedProbationLocation, time(9, 0), time(10, 0), LocationUsage.PROBATION),
        availableLocation(undecoratedLocation, time(9, 0), time(10, 0), LocationUsage.SHARED),
      )
    }

    @Test
    fun `should include ordered decorated probation team room, decorated probation room and shared room`() {
      whenever(locationsService.getVideoLinkLocationsAtPrison(WANDSWORTH, true)) doReturn listOf(decoratedProbationLocation, decoratedProbationTeamLocation, undecoratedLocation)
      whenever(
        bookedLocationsService.findBooked(
          BookedLookup(
            WANDSWORTH,
            tomorrow(),
            listOf(decoratedProbationLocation, decoratedProbationTeamLocation, undecoratedLocation),
          ),
        ),
      ) doReturn BookedLocations(emptyList())

      whenever(locationAttributesService.isLocationAvailableFor(LocationAvailableRequest.probation(1, BLACKPOOL_MC_PPOC, tomorrow().atTime(9, 0)))) doReturn AvailabilityStatus.PROBATION_ANY
      whenever(locationAttributesService.isLocationAvailableFor(LocationAvailableRequest.probation(2, BLACKPOOL_MC_PPOC, tomorrow().atTime(9, 0)))) doReturn AvailabilityStatus.PROBATION_ROOM

      val response = service().findAvailable(
        DateTimeAvailabilityRequest(
          prisonCode = WANDSWORTH,
          bookingType = BookingType.PROBATION,
          probationTeamCode = BLACKPOOL_MC_PPOC,
          date = tomorrow(),
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(10, 0),
        ),
      )

      response.locations hasSize 3

      response.locations containsExactly listOf(
        availableLocation(decoratedProbationTeamLocation, time(9, 0), time(10, 0), LocationUsage.PROBATION),
        availableLocation(decoratedProbationLocation, time(9, 0), time(10, 0), LocationUsage.PROBATION),
        availableLocation(undecoratedLocation, time(9, 0), time(10, 0), LocationUsage.SHARED),
      )
    }

    @Test
    fun `should favour decorated probation room over decorated probation team room when team room is not available`() {
      whenever(locationsService.getVideoLinkLocationsAtPrison(WANDSWORTH, true)) doReturn listOf(decoratedProbationLocation, decoratedProbationTeamLocation)
      whenever(
        bookedLocationsService.findBooked(
          BookedLookup(
            WANDSWORTH,
            tomorrow(),
            listOf(decoratedProbationLocation, decoratedProbationTeamLocation),
          ),
        ),
      ) doReturn BookedLocations(emptyList())

      whenever(locationAttributesService.isLocationAvailableFor(LocationAvailableRequest.probation(1, BLACKPOOL_MC_PPOC, tomorrow().atTime(9, 0)))) doReturn AvailabilityStatus.SHARED
      whenever(locationAttributesService.isLocationAvailableFor(LocationAvailableRequest.probation(2, BLACKPOOL_MC_PPOC, tomorrow().atTime(9, 0)))) doReturn AvailabilityStatus.NONE

      val response = service().findAvailable(
        DateTimeAvailabilityRequest(
          prisonCode = WANDSWORTH,
          bookingType = BookingType.PROBATION,
          probationTeamCode = BLACKPOOL_MC_PPOC,
          date = tomorrow(),
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(10, 0),
        ),
      )

      response.locations hasSize 1

      response.locations containsExactly listOf(
        availableLocation(decoratedProbationLocation, time(9, 0), time(10, 0), LocationUsage.PROBATION),
      )
    }

    @Test
    fun `should include decorated probation team room and shared room`() {
      whenever(locationsService.getVideoLinkLocationsAtPrison(WANDSWORTH, true)) doReturn listOf(decoratedProbationTeamLocation, undecoratedLocation)
      whenever(
        bookedLocationsService.findBooked(
          BookedLookup(
            WANDSWORTH,
            tomorrow(),
            listOf(decoratedProbationTeamLocation, undecoratedLocation),
          ),
        ),
      ) doReturn BookedLocations(emptyList())

      whenever(locationAttributesService.isLocationAvailableFor(LocationAvailableRequest.probation(2, BLACKPOOL_MC_PPOC, tomorrow().atTime(9, 0)))) doReturn AvailabilityStatus.PROBATION_ANY

      val response = service().findAvailable(
        DateTimeAvailabilityRequest(
          prisonCode = WANDSWORTH,
          bookingType = BookingType.PROBATION,
          probationTeamCode = BLACKPOOL_MC_PPOC,
          date = tomorrow(),
          startTime = LocalTime.of(9, 0),
          endTime = LocalTime.of(10, 0),
        ),
      )

      response.locations hasSize 2

      response.locations containsExactly listOf(
        availableLocation(decoratedProbationTeamLocation, time(9, 0), time(10, 0), LocationUsage.PROBATION),
        availableLocation(undecoratedLocation, time(9, 0), time(10, 0), LocationUsage.SHARED),
      )
    }
  }

  @Nested
  inner class AmendmentRequests {
    private val location1 = risleyLocation.toModel()
    private val location2 = risleyLocation2.toModel()

    @Test
    fun `should return 2 available morning times with time of request 10am`() {
      val booking = courtBooking().withMainCourtPrisonAppointment(date = today(), startTime = LocalTime.of(10, 0), endTime = LocalTime.of(11, 0), prisonCode = RISLEY, location = risleyLocation2)
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(booking.appointments().single())
      whenever(locationsService.getVideoLinkLocationsAtPrison(RISLEY, true)) doReturn listOf(location1, location2)
      whenever(bookedLocationsService.findBooked(BookedLookup(RISLEY, today(), listOf(location1, location2), booking.videoBookingId))) doReturn BookedLocations(
        listOf(
          BookedLocation(
            location2,
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
          ),
        ),
      )

      val response = service { LocalDate.now().atStartOfDay() }.findAvailable(
        DateTimeAvailabilityRequest(
          prisonCode = RISLEY,
          bookingType = BookingType.PROBATION,
          probationTeamCode = BLACKPOOL_MC_PPOC,
          date = today(),
          startTime = LocalTime.of(10, 0),
          endTime = LocalTime.of(11, 0),
          appointmentToExclude = 1,
        ),
      )

      response.locations hasSize 2

      response.locations containsExactly listOf(
        availableLocation(location1, time(10, 0), time(11, 0), LocationUsage.SHARED),
        availableLocation(location2, time(10, 0), time(11, 0), LocationUsage.SHARED),
      )
    }
  }

  private fun service(timeSource: TimeSource = TimeSource { LocalDateTime.now() }) = DateTimeAvailabilityService(
    locationsService,
    prisonAppointmentRepository,
    bookedLocationsService,
    locationAttributesService,
    timeSource,
  )

  private fun time(hour: Int, minute: Int) = LocalTime.of(hour, minute)

  private fun availableLocation(location: Location, startTime: LocalTime, endTime: LocalTime, usage: LocationUsage? = null) = AvailableLocation(
    name = location.description!!,
    startTime = startTime,
    endTime = endTime,
    dpsLocationKey = location.key,
    dpsLocationId = location.dpsLocationId,
    usage = usage?.let { LocationUsage.valueOf(usage.name) },
    timeSlot = slot(startTime),
  )
}

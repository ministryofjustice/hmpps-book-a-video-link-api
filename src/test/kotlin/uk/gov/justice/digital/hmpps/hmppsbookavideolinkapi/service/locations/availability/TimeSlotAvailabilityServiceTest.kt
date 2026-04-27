package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.availability

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.PrisonRegime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TimeSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.AvailabilityStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BLACKPOOL_MC_PPOC
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.RISLEY
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactly
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.risleyLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation2
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.RoomAttributes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.TimeSlot
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.TimeSlotAvailabilityRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AvailableLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.slot
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import java.time.LocalDateTime
import java.time.LocalTime

class TimeSlotAvailabilityServiceTest {
  private val locationsService: LocationsService = mock()
  private val bookedLocationsService: BookedLocationsService = mock()
  private val prisonRegime: PrisonRegime = mock()
  private val locationAttributesService: LocationAttributesAvailableService = mock()

  @DisplayName("Testing undecorated locations")
  @Nested
  inner class RequestsForUndecoratedRooms {
    private val location1 = risleyLocation.toModel()

    @BeforeEach
    fun before() {
      whenever(prisonRegime.startOfDay(RISLEY)) doReturn LocalTime.of(9, 0)
      whenever(prisonRegime.endOfDay(RISLEY)) doReturn LocalTime.of(17, 0)
    }

    @Test
    fun `should return 0 available times for an undecorated room`() {
      whenever(locationsService.getVideoLinkLocationsAtPrison(RISLEY, true)) doReturn listOf(location1)
      whenever(bookedLocationsService.findBooked(BookedLookup(RISLEY, tomorrow(), listOf(location1)))) doReturn BookedLocations(emptyList())

      val response = service { LocalDateTime.of(tomorrow(), LocalTime.of(10, 0)) }.findAvailable(
        TimeSlotAvailabilityRequest(
          prisonCode = RISLEY,
          bookingType = BookingType.PROBATION,
          probationTeamCode = BLACKPOOL_MC_PPOC,
          date = tomorrow(),
          bookingDuration = 60,
          timeSlots = listOf(TimeSlot.AM),
        ),
      )

      response.locations hasSize 0
    }
  }

  @DisplayName("Testing shared locations tomorrow")
  @Nested
  inner class RequestsForSharedRoomsTomorrow {

    private val location1 = wandsworthLocation.toModel().copy(
      description = "Decorated shared room",
      extraAttributes = RoomAttributes(
        attributeId = 1,
        locationStatus = LocationStatus.ACTIVE,
        locationUsage = LocationUsage.SHARED,
        allowedParties = emptyList(),
        prisonVideoUrl = null,
        notes = null,
      ),
    )

    @BeforeEach
    fun before() {
      whenever(prisonRegime.startOfDay(WANDSWORTH)) doReturn LocalTime.of(9, 0)
      whenever(prisonRegime.endOfDay(WANDSWORTH)) doReturn LocalTime.of(17, 0)
    }

    @Test
    fun `should return 5 available morning times for location 1 with one blocked out booking`() {
      whenever(locationsService.getVideoLinkLocationsAtPrison(WANDSWORTH, true)) doReturn listOf(location1)
      whenever(bookedLocationsService.findBooked(BookedLookup(WANDSWORTH, tomorrow(), listOf(location1)))) doReturn BookedLocations(emptyList())
      whenever(locationAttributesService.isLocationAvailableFor(any())) doReturn AvailabilityStatus.SHARED

      val response = service().findAvailable(
        TimeSlotAvailabilityRequest(
          prisonCode = WANDSWORTH,
          bookingType = BookingType.PROBATION,
          probationTeamCode = BLACKPOOL_MC_PPOC,
          date = tomorrow(),
          bookingDuration = 60,
          timeSlots = listOf(TimeSlot.AM),
        ),
      )

      response.locations hasSize 12

      response.locations containsExactly listOf(
        availableLocation(location1, time(9, 0), time(10, 0), LocationUsage.SHARED),
        availableLocation(location1, time(9, 15), time(10, 15), LocationUsage.SHARED),
        availableLocation(location1, time(9, 30), time(10, 30), LocationUsage.SHARED),
        availableLocation(location1, time(9, 45), time(10, 45), LocationUsage.SHARED),

        availableLocation(location1, time(10, 0), time(11, 0), LocationUsage.SHARED),
        availableLocation(location1, time(10, 15), time(11, 15), LocationUsage.SHARED),
        availableLocation(location1, time(10, 30), time(11, 30), LocationUsage.SHARED),
        availableLocation(location1, time(10, 45), time(11, 45), LocationUsage.SHARED),

        availableLocation(location1, time(11, 0), time(12, 0), LocationUsage.SHARED),
        availableLocation(location1, time(11, 15), time(12, 15), LocationUsage.SHARED),
        availableLocation(location1, time(11, 30), time(12, 30), LocationUsage.SHARED),
        availableLocation(location1, time(11, 45), time(12, 45), LocationUsage.SHARED),
      )
    }
  }

  @DisplayName("Testing decorated probation locations tomorrow")
  @Nested
  inner class RequestsForDecoratedRoomsTomorrow {

    private val location1 = wandsworthLocation.toModel().copy(
      description = "Decorated probation any room",
      extraAttributes = RoomAttributes(
        attributeId = 1,
        locationStatus = LocationStatus.ACTIVE,
        locationUsage = LocationUsage.PROBATION,
        allowedParties = emptyList(),
        prisonVideoUrl = null,
        notes = null,
      ),
    )
    private val location2 = wandsworthLocation2.toModel().copy(
      description = "Decorated probation team room",
      extraAttributes = RoomAttributes(
        attributeId = 2,
        locationStatus = LocationStatus.ACTIVE,
        locationUsage = LocationUsage.PROBATION,
        allowedParties = listOf("BLACKPOOL_MC_PPOC"),
        prisonVideoUrl = null,
        notes = null,
      ),
    )

    @BeforeEach
    fun before() {
      whenever(prisonRegime.startOfDay(WANDSWORTH)) doReturn LocalTime.of(9, 0)
      whenever(prisonRegime.endOfDay(WANDSWORTH)) doReturn LocalTime.of(17, 0)
    }

    @Test
    fun `should return 5 available morning times for location 1 with one blocked out booking`() {
      whenever(locationsService.getVideoLinkLocationsAtPrison(WANDSWORTH, true)) doReturn listOf(location1)
      whenever(bookedLocationsService.findBooked(BookedLookup(WANDSWORTH, tomorrow(), listOf(location1)))) doReturn BookedLocations(
        listOf(BookedLocation(location1, LocalTime.of(10, 0), LocalTime.of(11, 0))),
      )
      whenever(locationAttributesService.isLocationAvailableFor(any())) doReturn AvailabilityStatus.PROBATION_ANY

      val response = service().findAvailable(
        TimeSlotAvailabilityRequest(
          prisonCode = WANDSWORTH,
          bookingType = BookingType.PROBATION,
          probationTeamCode = BLACKPOOL_MC_PPOC,
          date = tomorrow(),
          bookingDuration = 60,
          timeSlots = listOf(TimeSlot.AM),
        ),
      )

      response.locations hasSize 5

      response.locations containsExactly listOf(
        availableLocation(location1, time(9, 0), time(10, 0), LocationUsage.PROBATION),
        availableLocation(location1, time(11, 0), time(12, 0), LocationUsage.PROBATION),
        availableLocation(location1, time(11, 15), time(12, 15), LocationUsage.PROBATION),
        availableLocation(location1, time(11, 30), time(12, 30), LocationUsage.PROBATION),
        availableLocation(location1, time(11, 45), time(12, 45), LocationUsage.PROBATION),
      )
    }

    @Test
    fun `should return 9 available morning times for location 1 and 2 with two blocked out bookings`() {
      whenever(locationsService.getVideoLinkLocationsAtPrison(WANDSWORTH, true)) doReturn listOf(location1, location2)
      whenever(
        bookedLocationsService.findBooked(
          BookedLookup(
            WANDSWORTH,
            tomorrow(),
            listOf(location1, location2),
          ),
        ),
      ) doReturn BookedLocations(
        listOf(
          BookedLocation(location1, LocalTime.of(10, 0), LocalTime.of(11, 0)),
          BookedLocation(location2, LocalTime.of(9, 0), LocalTime.of(10, 0)),
        ),
      )
      whenever(locationAttributesService.isLocationAvailableFor(any())) doReturn AvailabilityStatus.PROBATION_ANY

      val response = service().findAvailable(
        TimeSlotAvailabilityRequest(
          prisonCode = WANDSWORTH,
          bookingType = BookingType.PROBATION,
          probationTeamCode = BLACKPOOL_MC_PPOC,
          date = tomorrow(),
          bookingDuration = 60,
          timeSlots = listOf(TimeSlot.AM),
        ),
      )

      response.locations hasSize 9

      response.locations containsExactly listOf(
        availableLocation(location1, time(9, 0), time(10, 0), LocationUsage.PROBATION),
        availableLocation(location2, time(10, 0), time(11, 0), LocationUsage.PROBATION),
        availableLocation(location2, time(10, 15), time(11, 15), LocationUsage.PROBATION),
        availableLocation(location2, time(10, 30), time(11, 30), LocationUsage.PROBATION),
        availableLocation(location2, time(10, 45), time(11, 45), LocationUsage.PROBATION),
        availableLocation(location1, time(11, 0), time(12, 0), LocationUsage.PROBATION),
        availableLocation(location1, time(11, 15), time(12, 15), LocationUsage.PROBATION),
        availableLocation(location1, time(11, 30), time(12, 30), LocationUsage.PROBATION),
        availableLocation(location1, time(11, 45), time(12, 45), LocationUsage.PROBATION),
      )
    }

    @Test
    fun `should return 17 available afternoon times for location 1 when on blocked out booking`() {
      whenever(locationsService.getVideoLinkLocationsAtPrison(WANDSWORTH, true)) doReturn listOf(location1)
      whenever(bookedLocationsService.findBooked(BookedLookup(WANDSWORTH, tomorrow(), listOf(location1)))) doReturn BookedLocations(
        listOf(BookedLocation(location1, LocalTime.of(10, 0), LocalTime.of(11, 0))),
      )
      whenever(locationAttributesService.isLocationAvailableFor(any())) doReturn AvailabilityStatus.PROBATION_ANY

      val response = service().findAvailable(
        TimeSlotAvailabilityRequest(
          prisonCode = WANDSWORTH,
          bookingType = BookingType.PROBATION,
          probationTeamCode = BLACKPOOL_MC_PPOC,
          date = tomorrow(),
          bookingDuration = 60,
          timeSlots = listOf(TimeSlot.PM),
        ),
      )

      response.locations hasSize 17

      response.locations containsExactly listOf(
        availableLocation(location1, time(12, 0), time(13, 0), LocationUsage.PROBATION),
        availableLocation(location1, time(12, 15), time(13, 15), LocationUsage.PROBATION),
        availableLocation(location1, time(12, 30), time(13, 30), LocationUsage.PROBATION),
        availableLocation(location1, time(12, 45), time(13, 45), LocationUsage.PROBATION),
        availableLocation(location1, time(13, 0), time(14, 0), LocationUsage.PROBATION),
        availableLocation(location1, time(13, 15), time(14, 15), LocationUsage.PROBATION),
        availableLocation(location1, time(13, 30), time(14, 30), LocationUsage.PROBATION),
        availableLocation(location1, time(13, 45), time(14, 45), LocationUsage.PROBATION),
        availableLocation(location1, time(14, 0), time(15, 0), LocationUsage.PROBATION),
        availableLocation(location1, time(14, 15), time(15, 15), LocationUsage.PROBATION),
        availableLocation(location1, time(14, 30), time(15, 30), LocationUsage.PROBATION),
        availableLocation(location1, time(14, 45), time(15, 45), LocationUsage.PROBATION),
        availableLocation(location1, time(15, 0), time(16, 0), LocationUsage.PROBATION),
        availableLocation(location1, time(15, 15), time(16, 15), LocationUsage.PROBATION),
        availableLocation(location1, time(15, 30), time(16, 30), LocationUsage.PROBATION),
        availableLocation(location1, time(15, 45), time(16, 45), LocationUsage.PROBATION),
        availableLocation(location1, time(16, 0), time(17, 0), LocationUsage.PROBATION),
      )
    }

    @Test
    fun `should return 15 available morning and afternoon times for location 1 when two block out bookings`() {
      whenever(locationsService.getVideoLinkLocationsAtPrison(WANDSWORTH, true)) doReturn listOf(location1)
      whenever(bookedLocationsService.findBooked(BookedLookup(WANDSWORTH, tomorrow(), listOf(location1)))) doReturn BookedLocations(
        listOf(
          BookedLocation(location1, LocalTime.of(10, 0), LocalTime.of(11, 0)),
          BookedLocation(location1, LocalTime.of(13, 0), LocalTime.of(14, 0)),
        ),
      )
      whenever(locationAttributesService.isLocationAvailableFor(any())) doReturn AvailabilityStatus.PROBATION_ANY

      val response = service().findAvailable(
        TimeSlotAvailabilityRequest(
          prisonCode = WANDSWORTH,
          bookingType = BookingType.PROBATION,
          probationTeamCode = BLACKPOOL_MC_PPOC,
          date = tomorrow(),
          bookingDuration = 60,
          timeSlots = listOf(TimeSlot.AM, TimeSlot.PM),
        ),
      )

      response.locations hasSize 15

      response.locations containsExactly listOf(
        availableLocation(location1, time(9, 0), time(10, 0), LocationUsage.PROBATION),
        availableLocation(location1, time(11, 0), time(12, 0), LocationUsage.PROBATION),
        availableLocation(location1, time(11, 15), time(12, 15), LocationUsage.PROBATION),
        availableLocation(location1, time(11, 30), time(12, 30), LocationUsage.PROBATION),
        availableLocation(location1, time(11, 45), time(12, 45), LocationUsage.PROBATION),
        availableLocation(location1, time(12, 0), time(13, 0), LocationUsage.PROBATION),
        availableLocation(location1, time(14, 0), time(15, 0), LocationUsage.PROBATION),
        availableLocation(location1, time(14, 15), time(15, 15), LocationUsage.PROBATION),
        availableLocation(location1, time(14, 30), time(15, 30), LocationUsage.PROBATION),
        availableLocation(location1, time(14, 45), time(15, 45), LocationUsage.PROBATION),
        availableLocation(location1, time(15, 0), time(16, 0), LocationUsage.PROBATION),
        availableLocation(location1, time(15, 15), time(16, 15), LocationUsage.PROBATION),
        availableLocation(location1, time(15, 30), time(16, 30), LocationUsage.PROBATION),
        availableLocation(location1, time(15, 45), time(16, 45), LocationUsage.PROBATION),
        availableLocation(location1, time(16, 0), time(17, 0), LocationUsage.PROBATION),
      )
    }
  }

  @Nested
  inner class UndecoratedAndDecoratedRooms {
    private val decoratedProbationLocation = wandsworthLocation.toModel().copy(
      description = "b - decorated probation room",
      extraAttributes = RoomAttributes(
        attributeId = 1,
        locationStatus = LocationStatus.ACTIVE,
        locationUsage = LocationUsage.PROBATION,
        allowedParties = emptyList(),
        prisonVideoUrl = null,
        notes = null,
      ),
    )

    private val decoratedProbationTeamLocation = wandsworthLocation.toModel().copy(
      description = "c - decorated probation team room",
      extraAttributes = RoomAttributes(
        attributeId = 2,
        locationStatus = LocationStatus.ACTIVE,
        locationUsage = LocationUsage.PROBATION,
        allowedParties = listOf("BLACKPOOL_MC_PPOC"),
        prisonVideoUrl = null,
        notes = null,
      ),
    )

    private val undecoratedLocation = wandsworthLocation2.toModel().copy(description = "a - undecorated room")

    @BeforeEach
    fun before() {
      whenever(prisonRegime.startOfDay(WANDSWORTH)) doReturn LocalTime.of(9, 0)
      whenever(prisonRegime.endOfDay(WANDSWORTH)) doReturn LocalTime.of(9, 45)
    }

    @Test
    fun `should only offer the decorated probation rooms`() {
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

      whenever(locationAttributesService.isLocationAvailableFor(LocationAvailableRequest.probation(1, BLACKPOOL_MC_PPOC, tomorrow(), LocalTime.of(9, 0), LocalTime.of(9, 30)))) doReturn AvailabilityStatus.PROBATION_ANY
      whenever(locationAttributesService.isLocationAvailableFor(LocationAvailableRequest.probation(1, BLACKPOOL_MC_PPOC, tomorrow(), LocalTime.of(9, 15), LocalTime.of(9, 45)))) doReturn AvailabilityStatus.PROBATION_ANY

      val response = service().findAvailable(
        TimeSlotAvailabilityRequest(
          prisonCode = WANDSWORTH,
          bookingType = BookingType.PROBATION,
          probationTeamCode = BLACKPOOL_MC_PPOC,
          date = tomorrow(),
          bookingDuration = 30,
          timeSlots = listOf(TimeSlot.AM),
        ),
      )

      response.locations hasSize 2

      response.locations containsExactly listOf(
        availableLocation(decoratedProbationLocation, time(9, 0), time(9, 30), LocationUsage.PROBATION),
        availableLocation(decoratedProbationLocation, time(9, 15), time(9, 45), LocationUsage.PROBATION),
      )
    }

    @Test
    fun `should favour decorated team room over a decorated probation any room`() {
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

      whenever(locationAttributesService.isLocationAvailableFor(LocationAvailableRequest.probation(1, BLACKPOOL_MC_PPOC, tomorrow(), LocalTime.of(9, 0), LocalTime.of(9, 30)))) doReturn AvailabilityStatus.PROBATION_ANY
      whenever(locationAttributesService.isLocationAvailableFor(LocationAvailableRequest.probation(1, BLACKPOOL_MC_PPOC, tomorrow(), LocalTime.of(9, 15), LocalTime.of(9, 45)))) doReturn AvailabilityStatus.PROBATION_ANY
      whenever(locationAttributesService.isLocationAvailableFor(LocationAvailableRequest.probation(2, BLACKPOOL_MC_PPOC, tomorrow(), LocalTime.of(9, 0), LocalTime.of(9, 30)))) doReturn AvailabilityStatus.PROBATION_ROOM
      whenever(locationAttributesService.isLocationAvailableFor(LocationAvailableRequest.probation(2, BLACKPOOL_MC_PPOC, tomorrow(), LocalTime.of(9, 15), LocalTime.of(9, 45)))) doReturn AvailabilityStatus.PROBATION_ROOM

      val response = service().findAvailable(
        TimeSlotAvailabilityRequest(
          prisonCode = WANDSWORTH,
          bookingType = BookingType.PROBATION,
          probationTeamCode = BLACKPOOL_MC_PPOC,
          date = tomorrow(),
          bookingDuration = 30,
          timeSlots = listOf(TimeSlot.AM),
        ),
      )

      response.locations hasSize 2

      response.locations containsExactly listOf(
        availableLocation(decoratedProbationTeamLocation, time(9, 0), time(9, 30), LocationUsage.PROBATION),
        availableLocation(decoratedProbationTeamLocation, time(9, 15), time(9, 45), LocationUsage.PROBATION),
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

      whenever(locationAttributesService.isLocationAvailableFor(LocationAvailableRequest.probation(1, BLACKPOOL_MC_PPOC, tomorrow(), LocalTime.of(9, 0), LocalTime.of(9, 30)))) doReturn AvailabilityStatus.PROBATION_ANY
      whenever(locationAttributesService.isLocationAvailableFor(LocationAvailableRequest.probation(1, BLACKPOOL_MC_PPOC, tomorrow(), LocalTime.of(9, 15), LocalTime.of(9, 45)))) doReturn AvailabilityStatus.PROBATION_ANY
      whenever(locationAttributesService.isLocationAvailableFor(LocationAvailableRequest.probation(2, BLACKPOOL_MC_PPOC, tomorrow(), LocalTime.of(9, 0), LocalTime.of(9, 30)))) doReturn AvailabilityStatus.NONE
      whenever(locationAttributesService.isLocationAvailableFor(LocationAvailableRequest.probation(2, BLACKPOOL_MC_PPOC, tomorrow(), LocalTime.of(9, 15), LocalTime.of(9, 45)))) doReturn AvailabilityStatus.NONE

      val response = service().findAvailable(
        TimeSlotAvailabilityRequest(
          prisonCode = WANDSWORTH,
          bookingType = BookingType.PROBATION,
          probationTeamCode = BLACKPOOL_MC_PPOC,
          date = tomorrow(),
          bookingDuration = 30,
          timeSlots = listOf(TimeSlot.AM),
        ),
      )

      response.locations hasSize 2

      response.locations containsExactly listOf(
        availableLocation(decoratedProbationLocation, time(9, 0), time(9, 30), LocationUsage.PROBATION),
        availableLocation(decoratedProbationLocation, time(9, 15), time(9, 45), LocationUsage.PROBATION),
      )
    }

    @Test
    fun `should favour decorated probation team room over decorated probation room over shared room`() {
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

      whenever(locationAttributesService.isLocationAvailableFor(LocationAvailableRequest.probation(1, BLACKPOOL_MC_PPOC, tomorrow(), LocalTime.of(9, 0), LocalTime.of(9, 30)))) doReturn AvailabilityStatus.PROBATION_ANY
      whenever(locationAttributesService.isLocationAvailableFor(LocationAvailableRequest.probation(1, BLACKPOOL_MC_PPOC, tomorrow(), LocalTime.of(9, 15), LocalTime.of(9, 45)))) doReturn AvailabilityStatus.PROBATION_ANY
      whenever(locationAttributesService.isLocationAvailableFor(LocationAvailableRequest.probation(2, BLACKPOOL_MC_PPOC, tomorrow(), LocalTime.of(9, 0), LocalTime.of(9, 30)))) doReturn AvailabilityStatus.PROBATION_ROOM
      whenever(locationAttributesService.isLocationAvailableFor(LocationAvailableRequest.probation(2, BLACKPOOL_MC_PPOC, tomorrow(), LocalTime.of(9, 15), LocalTime.of(9, 45)))) doReturn AvailabilityStatus.PROBATION_ROOM

      val response = service().findAvailable(
        TimeSlotAvailabilityRequest(
          prisonCode = WANDSWORTH,
          bookingType = BookingType.PROBATION,
          probationTeamCode = BLACKPOOL_MC_PPOC,
          date = tomorrow(),
          bookingDuration = 30,
          timeSlots = listOf(TimeSlot.AM),
        ),
      )

      response.locations hasSize 2

      response.locations containsExactly listOf(
        availableLocation(decoratedProbationTeamLocation, time(9, 0), time(9, 30), LocationUsage.PROBATION),
        availableLocation(decoratedProbationTeamLocation, time(9, 15), time(9, 45), LocationUsage.PROBATION),
      )
    }

    @Test
    fun `should exclude decorated probation room at 9am but include decorated room at 915am`() {
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

      whenever(locationAttributesService.isLocationAvailableFor(LocationAvailableRequest.probation(1, BLACKPOOL_MC_PPOC, tomorrow(), LocalTime.of(9, 0), LocalTime.of(9, 30)))) doReturn AvailabilityStatus.NONE
      whenever(locationAttributesService.isLocationAvailableFor(LocationAvailableRequest.probation(1, BLACKPOOL_MC_PPOC, tomorrow(), LocalTime.of(9, 15), LocalTime.of(9, 45)))) doReturn AvailabilityStatus.PROBATION_ANY

      val response = service().findAvailable(
        TimeSlotAvailabilityRequest(
          prisonCode = WANDSWORTH,
          bookingType = BookingType.PROBATION,
          probationTeamCode = BLACKPOOL_MC_PPOC,
          date = tomorrow(),
          bookingDuration = 30,
          timeSlots = listOf(TimeSlot.AM),
        ),
      )

      response.locations hasSize 1

      // Never provide an undecorated room
      // Decorated location is busy at 9.00 but free at 9:15
      response.locations containsExactly listOf(
        availableLocation(decoratedProbationLocation, time(9, 15), time(9, 45), LocationUsage.PROBATION),
      )
    }
  }

  private fun service(timeSource: TimeSource = TimeSource { LocalDateTime.now() }) = TimeSlotAvailabilityService(
    locationsService,
    bookedLocationsService,
    prisonRegime,
    timeSource,
    locationAttributesService,
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

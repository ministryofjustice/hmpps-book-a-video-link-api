package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.availability

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendCourtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendProbationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withProbationPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AvailabilityRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.Interval
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.LocationAndInterval
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.BookingStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.availability.AvailabilityFinderService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.availability.AvailabilityOptionsGenerator
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.availability.AvailabilityService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.availability.ExternalAppointmentsService
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional
import java.util.UUID

class AvailabilityServiceTest {
  private val videoAppointmentRepository: VideoAppointmentRepository = mock()
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient = mock()
  private val externalAppointmentsService: ExternalAppointmentsService = mock()
  private val videoBookingRepository: VideoBookingRepository = mock()

  private val availabilityOptionsGenerator = AvailabilityOptionsGenerator(
    dayStart = LocalTime.of(9, 0),
    dayEnd = LocalTime.of(16, 0),
    step = Duration.parse("PT15M"),
  )

  private val availabilityFinderService = AvailabilityFinderService(availabilityOptionsGenerator)

  private val service = AvailabilityService(
    videoAppointmentRepository,
    locationsInsidePrisonClient,
    availabilityFinderService,
    externalAppointmentsService,
    videoBookingRepository,
  )

  private fun createVideoAppointment(
    videoBookId: Long,
    appId: Long,
    locationId: UUID,
    appType: String,
    startTime: LocalTime,
    endTime: LocalTime,
  ) = VideoAppointment(
    videoBookingId = videoBookId,
    prisonAppointmentId = appId,
    bookingType = BookingType.COURT.name,
    statusCode = BookingStatus.ACTIVE.name,
    courtCode = "TESTC",
    probationTeamCode = null,
    prisonCode = WANDSWORTH,
    prisonerNumber = "A1234AA",
    appointmentType = appType,
    prisonLocationId = locationId,
    appointmentDate = LocalDate.now(),
    startTime = startTime,
    endTime = endTime,
    lastCreatedOrAmended = LocalDateTime.now(),
  )

  private val room1 = location(WANDSWORTH, "VCC-1")
  private val room2 = location(WANDSWORTH, "VCC-2")
  private val room3 = location(WANDSWORTH, "VCC-3")

  private val videoAppointments = listOf(
    createVideoAppointment(1L, 1L, room1.id, "VLB_COURT_PRE", LocalTime.of(9, 15), LocalTime.of(9, 30)),
    createVideoAppointment(1L, 2L, room1.id, "VLB_COURT_MAIN", LocalTime.of(9, 30), LocalTime.of(10, 0)),
    createVideoAppointment(2L, 3L, room1.id, "VLB_COURT_MAIN", LocalTime.of(10, 0), LocalTime.of(11, 0)),
    createVideoAppointment(2L, 4L, room1.id, "VLB_COURT_POST", LocalTime.of(11, 0), LocalTime.of(11, 15)),
    createVideoAppointment(3L, 5L, room1.id, "VLB_COURT_MAIN", LocalTime.of(11, 15), LocalTime.of(11, 45)),
    createVideoAppointment(4L, 6L, room2.id, "VLB_COURT_MAIN", LocalTime.of(9, 30), LocalTime.of(12, 30)),
    createVideoAppointment(5L, 7L, room2.id, "VLB_COURT_MAIN", LocalTime.of(13, 30), LocalTime.of(16, 30)),
    createVideoAppointment(6L, 8L, room3.id, "VLB_COURT_MAIN", LocalTime.of(9, 0), LocalTime.of(19, 0)),
  )

  @BeforeEach
  fun setUpMock() {
    // Mock the view of existing video appointments
    whenever(
      videoAppointmentRepository.findVideoAppointmentsAtPrison(
        forDate = any(),
        forPrison = any(),
        forLocationIds = anyList(),
      ),
    ).thenReturn(videoAppointments)
  }

  @Test
  fun `No options when the requested time is free`() {
    // Request for a time which is currently free
    val request = AvailabilityRequest(
      bookingType = BookingType.COURT,
      courtOrProbationCode = "TESTC",
      prisonCode = WANDSWORTH,
      date = LocalDate.now(),
      mainAppointment = LocationAndInterval(
        prisonLocKey = room1.key,
        interval = Interval(start = LocalTime.of(12, 0), end = LocalTime.of(12, 30)),
      ),
    )

    whenever(locationsInsidePrisonClient.getLocationsByKeys(any())) doReturn listOf(room1, room2, room3)

    val response = service.checkAvailability(request)

    assertThat(response).isNotNull
    with(response) {
      assertThat(availabilityOk).isTrue()
      assertThat(alternatives).hasSize(0)
    }
  }

  @Test
  fun `Options provided when the requested time is not free`() {
    // Request for a time already taken
    val request = AvailabilityRequest(
      bookingType = BookingType.COURT,
      courtOrProbationCode = "TESTC",
      prisonCode = WANDSWORTH,
      date = LocalDate.now(),
      mainAppointment = LocationAndInterval(
        prisonLocKey = room1.key,
        interval = Interval(start = LocalTime.of(11, 0), end = LocalTime.of(11, 30)),
      ),
    )

    whenever(locationsInsidePrisonClient.getLocationsByKeys(any())) doReturn listOf(room1, room2, room3)

    val response = service.checkAvailability(request)

    assertThat(response).isNotNull
    with(response) {
      assertThat(availabilityOk).isFalse()
      assertThat(alternatives).hasSize(3)
      assertThat(alternatives).extracting("main").containsAll(
        listOf(
          LocationAndInterval(
            prisonLocKey = room1.key,
            interval = Interval(start = LocalTime.of(11, 45), end = LocalTime.of(12, 15)),
          ),
          LocationAndInterval(
            prisonLocKey = room1.key,
            interval = Interval(start = LocalTime.of(12, 0), end = LocalTime.of(12, 30)),
          ),
          LocationAndInterval(
            prisonLocKey = room1.key,
            interval = Interval(start = LocalTime.of(12, 15), end = LocalTime.of(12, 45)),
          ),
        ),
      )
    }
  }

  @Test
  fun `Considers all rooms when multiple rooms are requested and one is unavailable`() {
    // Request for multiple rooms with one of them busy at this time
    val request = AvailabilityRequest(
      bookingType = BookingType.COURT,
      courtOrProbationCode = "TESTC",
      prisonCode = WANDSWORTH,
      date = LocalDate.now(),
      preAppointment = LocationAndInterval(
        prisonLocKey = room1.key,
        interval = Interval(start = LocalTime.of(14, 0), end = LocalTime.of(14, 15)),
      ),
      mainAppointment = LocationAndInterval(
        prisonLocKey = room2.key,
        interval = Interval(start = LocalTime.of(14, 15), end = LocalTime.of(14, 45)),
      ),
      postAppointment = LocationAndInterval(
        prisonLocKey = room1.key,
        interval = Interval(start = LocalTime.of(14, 45), end = LocalTime.of(15, 0)),
      ),
    )

    whenever(locationsInsidePrisonClient.getLocationsByKeys(any())) doReturn listOf(room1, room2, room3)

    val response = service.checkAvailability(request)

    assertThat(response).isNotNull
    with(response) {
      assertThat(availabilityOk).isFalse()
      assertThat(alternatives).hasSize(3)
      assertThat(alternatives).extracting("main").containsAll(
        listOf(
          LocationAndInterval(
            prisonLocKey = room2.key,
            interval = Interval(start = LocalTime.of(12, 30), end = LocalTime.of(13, 0)),
          ),
          LocationAndInterval(
            prisonLocKey = room2.key,
            interval = Interval(start = LocalTime.of(12, 45), end = LocalTime.of(13, 15)),
          ),
          LocationAndInterval(
            prisonLocKey = room2.key,
            interval = Interval(start = LocalTime.of(13, 0), end = LocalTime.of(13, 30)),
          ),
        ),
      )
    }
  }

  @Test
  fun `No options are provided when room is busy and requested times are outside the start and end day times`() {
    // Request for a time outside the start/end times of the day
    val request = AvailabilityRequest(
      bookingType = BookingType.COURT,
      courtOrProbationCode = "TESTC",
      prisonCode = WANDSWORTH,
      date = LocalDate.now(),
      mainAppointment = LocationAndInterval(
        prisonLocKey = room3.key,
        interval = Interval(start = LocalTime.of(18, 0), end = LocalTime.of(19, 0)),
      ),
    )

    whenever(locationsInsidePrisonClient.getLocationsByKeys(any())) doReturn listOf(room1, room2, room3)

    val response = service.checkAvailability(request)

    // Room3 is busy all day
    assertThat(response).isNotNull
    with(response) {
      assertThat(availabilityOk).isFalse()
      assertThat(alternatives).hasSize(0)
    }
  }

  @Test
  fun `Excludes appointments with the same videoBookingId as vlbIdToExclude`() {
    // Request to exclude certain videoBookingId
    val request = AvailabilityRequest(
      bookingType = BookingType.COURT,
      courtOrProbationCode = "TESTC",
      prisonCode = WANDSWORTH,
      date = LocalDate.now(),
      mainAppointment = LocationAndInterval(
        prisonLocKey = room1.key,
        interval = Interval(start = LocalTime.of(10, 0), end = LocalTime.of(11, 0)),
      ),
      vlbIdToExclude = 2L,
    )

    whenever(locationsInsidePrisonClient.getLocationsByKeys(any())) doReturn listOf(room1, room2, room3)

    val response = service.checkAvailability(request)

    // We should exclude appointments with videoBookingId == 2L
    assertThat(response).isNotNull
    with(response) {
      assertThat(availabilityOk).isTrue()
      assertThat(alternatives).hasSize(0) // No conflicts after excluding
    }
  }

  @Test
  fun `Includes appointments not matching vlbIdToExclude`() {
    // Request to exclude certain videoBookingId
    val request = AvailabilityRequest(
      bookingType = BookingType.COURT,
      courtOrProbationCode = "TESTC",
      prisonCode = WANDSWORTH,
      date = LocalDate.now(),
      mainAppointment = LocationAndInterval(
        prisonLocKey = room1.key,
        interval = Interval(start = LocalTime.of(10, 0), end = LocalTime.of(11, 0)),
      ),
      vlbIdToExclude = 5L,
    )

    whenever(locationsInsidePrisonClient.getLocationsByKeys(any())) doReturn listOf(room1, room2, room3)

    val response = service.checkAvailability(request)

    // We should not exclude appointments with videoBookingId == 2L (conflict exists)
    assertThat(response).isNotNull
    with(response) {
      assertThat(availabilityOk).isFalse()
      assertThat(alternatives).hasSize(3)
    }
  }

  @Test
  fun `should be available if request matches existing court bookings pre, main and post booking date, time and location`() {
    val existingCourtBookingRequest = AvailabilityRequest(
      bookingType = BookingType.COURT,
      courtOrProbationCode = "TESTC",
      prisonCode = WANDSWORTH,
      date = today(),
      preAppointment = LocationAndInterval(
        prisonLocKey = room1.key,
        interval = Interval(start = LocalTime.of(9, 0), end = LocalTime.of(10, 0)),
      ),
      mainAppointment = LocationAndInterval(
        prisonLocKey = room1.key,
        interval = Interval(start = LocalTime.of(10, 0), end = LocalTime.of(11, 0)),
      ),
      postAppointment = LocationAndInterval(
        prisonLocKey = room1.key,
        interval = Interval(start = LocalTime.of(11, 0), end = LocalTime.of(12, 0)),
      ),
      vlbIdToExclude = 2L,
    )

    whenever(videoBookingRepository.findById(2L)) doReturn Optional.of(
      courtBooking().withMainCourtPrisonAppointment(
        date = today(),
        prisonCode = WANDSWORTH,
        prisonerNumber = "123456",
        location = room1,
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(11, 0),
      ).addAppointment(
        prison = prison(prisonCode = WANDSWORTH),
        prisonerNumber = "123456",
        appointmentType = "VLB_COURT_PRE",
        date = today(),
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(10, 0),
        locationId = room1.id,
      ).addAppointment(
        prison = prison(prisonCode = WANDSWORTH),
        prisonerNumber = "123456",
        appointmentType = "VLB_COURT_POST",
        date = today(),
        startTime = LocalTime.of(11, 0),
        endTime = LocalTime.of(12, 0),
        locationId = room1.id,
      ),
    )
    whenever(locationsInsidePrisonClient.getLocationsByKeys(any())) doReturn listOf(room1)

    with(service.checkAvailability(existingCourtBookingRequest)) {
      availabilityOk isBool true
      alternatives hasSize 0
    }

    verifyNoInteractions(videoAppointmentRepository)
    verifyNoInteractions(externalAppointmentsService)
  }

  @Test
  fun `should be available if request matches existing court booking main hearing booking date, time and location`() {
    val existingCourtBookingRequest = AvailabilityRequest(
      bookingType = BookingType.COURT,
      courtOrProbationCode = "TESTC",
      prisonCode = WANDSWORTH,
      date = today(),
      mainAppointment = LocationAndInterval(
        prisonLocKey = room1.key,
        interval = Interval(start = LocalTime.of(10, 0), end = LocalTime.of(11, 0)),
      ),
      vlbIdToExclude = 2L,
    )

    whenever(videoBookingRepository.findById(2L)) doReturn Optional.of(
      courtBooking().withMainCourtPrisonAppointment(
        date = today(),
        prisonCode = WANDSWORTH,
        location = room1,
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(11, 0),
      ),
    )
    whenever(locationsInsidePrisonClient.getLocationsByKeys(any())) doReturn listOf(room1)

    with(service.checkAvailability(existingCourtBookingRequest)) {
      availabilityOk isBool true
      alternatives hasSize 0
    }

    verifyNoInteractions(videoAppointmentRepository)
    verifyNoInteractions(externalAppointmentsService)
  }

  @Test
  fun `should be available if request matches existing probation booking meeting date, time and location`() {
    val existingProbationBookingRequest = AvailabilityRequest(
      bookingType = BookingType.PROBATION,
      courtOrProbationCode = "TESTP",
      prisonCode = WANDSWORTH,
      date = today(),
      mainAppointment = LocationAndInterval(
        prisonLocKey = room2.key,
        interval = Interval(start = LocalTime.of(11, 0), end = LocalTime.of(12, 0)),
      ),
      vlbIdToExclude = 3L,
    )

    whenever(videoBookingRepository.findById(3L)) doReturn Optional.of(
      probationBooking().withProbationPrisonAppointment(
        date = today(),
        prisonCode = WANDSWORTH,
        location = room2,
        startTime = LocalTime.of(11, 0),
        endTime = LocalTime.of(12, 0),
      ),
    )
    whenever(locationsInsidePrisonClient.getLocationsByKeys(any())) doReturn listOf(room2)

    with(service.checkAvailability(existingProbationBookingRequest)) {
      availabilityOk isBool true
      alternatives hasSize 0
    }

    verifyNoInteractions(videoAppointmentRepository)
    verifyNoInteractions(externalAppointmentsService)
  }

  @Test
  fun `is available if request matches existing court booking main hearing booking date, time and location`() {
    whenever(locationsInsidePrisonClient.getLocationsByKeys(any())) doReturn listOf(room1)

    val request = amendCourtBookingRequest(
      prisonCode = WANDSWORTH,
      appointmentDate = today(),
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 0),
      location = room1,
    )

    whenever(videoBookingRepository.findById(2L)) doReturn Optional.of(
      courtBooking().withMainCourtPrisonAppointment(
        date = today(),
        prisonCode = WANDSWORTH,
        location = room1,
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(11, 0),
      ),
    )

    service.isAvailable(2, request) isBool true

    verifyNoInteractions(videoAppointmentRepository)
    verifyNoInteractions(externalAppointmentsService)
  }

  @Test
  fun `is available if request matches existing probation booking meeting date, time and location`() {
    whenever(locationsInsidePrisonClient.getLocationsByKeys(any())) doReturn listOf(room2)

    val request = amendProbationBookingRequest(
      prisonCode = WANDSWORTH,
      appointmentDate = today(),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(12, 0),
      location = room2,
    )

    whenever(videoBookingRepository.findById(3L)) doReturn Optional.of(
      probationBooking().withProbationPrisonAppointment(
        date = today(),
        prisonCode = WANDSWORTH,
        location = room2,
        startTime = LocalTime.of(11, 0),
        endTime = LocalTime.of(12, 0),
      ),
    )

    service.isAvailable(3, request) isBool true

    verifyNoInteractions(videoAppointmentRepository)
    verifyNoInteractions(externalAppointmentsService)
  }
}

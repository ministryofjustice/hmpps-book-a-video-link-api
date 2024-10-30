package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AvailabilityRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.Interval
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.LocationAndInterval
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.BookingStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoAppointmentRepository
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import org.mockito.kotlin.doReturn
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.location

class AvailabilityServiceTest {
  private val videoAppointmentRepository: VideoAppointmentRepository = mock()
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient = mock()

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
  )

  private fun createVideoAppointment(
    videoBookId: Long,
    appId: Long,
    locationId: String,
    appType: String,
    startTime: LocalTime,
    endTime: LocalTime,
  ) =
    VideoAppointment(
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
    )

  private val room1 = location(WANDSWORTH, "VCC-1")
  private val room2 = location(WANDSWORTH, "VCC-2")
  private val room3 = location(WANDSWORTH, "VCC-3")

  private val videoAppointments = listOf(
    createVideoAppointment(1L, 1L, room1.id.toString(), "VLB_COURT_PRE", LocalTime.of(9, 15), LocalTime.of(9, 30)),
    createVideoAppointment(1L, 2L, room1.id.toString(), "VLB_COURT_MAIN", LocalTime.of(9, 30), LocalTime.of(10, 0)),
    createVideoAppointment(2L, 3L, room1.id.toString(),  "VLB_COURT_MAIN", LocalTime.of(10, 0), LocalTime.of(11, 0)),
    createVideoAppointment(2L, 4L, room1.id.toString(),  "VLB_COURT_POST", LocalTime.of(11, 0), LocalTime.of(11, 15)),
    createVideoAppointment(3L, 5L, room1.id.toString(),  "VLB_COURT_MAIN", LocalTime.of(11, 15), LocalTime.of(11, 45)),
    createVideoAppointment(4L, 6L, room2.id.toString(),  "VLB_COURT_MAIN", LocalTime.of(9, 30), LocalTime.of(12, 30)),
    createVideoAppointment(5L, 7L, room2.id.toString(),  "VLB_COURT_MAIN", LocalTime.of(13, 30), LocalTime.of(16, 30)),
    createVideoAppointment(6L, 8L, room3.id.toString(),  "VLB_COURT_MAIN", LocalTime.of(9, 0), LocalTime.of(19, 0)),
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
        prisonLocKey =  room1.key,
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
            prisonLocKey =  room1.key,
            interval = Interval(start = LocalTime.of(11, 45), end = LocalTime.of(12, 15)),
          ),
          LocationAndInterval(
            prisonLocKey =  room1.key,
            interval = Interval(start = LocalTime.of(12, 0), end = LocalTime.of(12, 30)),
          ),
          LocationAndInterval(
            prisonLocKey =  room1.key,
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
        prisonLocKey =  room1.key,
        interval = Interval(start = LocalTime.of(14, 0), end = LocalTime.of(14, 15)),
      ),
      mainAppointment = LocationAndInterval(
        prisonLocKey =  room2.key,
        interval = Interval(start = LocalTime.of(14, 15), end = LocalTime.of(14, 45)),
      ),
      postAppointment = LocationAndInterval(
        prisonLocKey =  room1.key,
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
            prisonLocKey =  room2.key,
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
}

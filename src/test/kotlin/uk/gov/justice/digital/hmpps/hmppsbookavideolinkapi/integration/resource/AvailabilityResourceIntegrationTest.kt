package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvilleLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AvailabilityRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.Interval
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.LocationAndInterval
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AvailabilityResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import java.time.LocalDate
import java.time.LocalTime

class AvailabilityResourceIntegrationTest : IntegrationTestBase() {
  @Autowired
  private lateinit var videoBookingRepository: VideoBookingRepository

  @Test
  fun `should confirm availability for a court booking when the prison room is free`() {
    videoBookingRepository.findAll() hasSize 0

    // With no bookings present, the availability check should succeed
    val availabilityRequest = AvailabilityRequest(
      bookingType = BookingType.COURT,
      courtOrProbationCode = "DRBYMC",
      prisonCode = PENTONVILLE,
      date = LocalDate.now().plusDays(1),
      mainAppointment = LocationAndInterval(
        pentonvilleLocation.key,
        Interval(
          start = LocalTime.of(12, 0),
          end = LocalTime.of(12, 30),
        ),
      ),
    )

    val availabilityResponse = webTestClient.availabilityCheck(availabilityRequest)

    assertThat(availabilityResponse).isNotNull
    with(availabilityResponse) {
      assertThat(availabilityOk).isTrue()
      assertThat(alternatives).isEmpty()
    }
  }

  @Test
  fun `should offer alternatives for a court booking when the prison room is already occupied`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("A1111AA", PENTONVILLE)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(pentonvilleLocation.key), PENTONVILLE)

    val courtBookingRequest = courtBookingRequest(
      courtCode = "DRBYMC",
      prisonerNumber = "A1111AA",
      prisonCode = PENTONVILLE,
      location = pentonvilleLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    webTestClient.createBooking(courtBookingRequest, COURT_USER)

    // Do the availability check for a booking at the same time as the existing booking above
    val availabilityRequest = AvailabilityRequest(
      bookingType = BookingType.COURT,
      courtOrProbationCode = "DRBYMC",
      prisonCode = PENTONVILLE,
      date = LocalDate.now().plusDays(1),
      mainAppointment = LocationAndInterval(
        pentonvilleLocation.key,
        Interval(
          start = LocalTime.of(12, 0),
          end = LocalTime.of(12, 30),
        ),
      ),
    )

    val availabilityResponse = webTestClient.availabilityCheck(availabilityRequest)

    assertThat(availabilityResponse).isNotNull
    with(availabilityResponse) {
      assertThat(availabilityOk).isFalse()
      assertThat(alternatives).hasSize(3)
    }
  }

  @Test
  fun `should exclude specific booking when checking for availability`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("A1111AA", PENTONVILLE)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(pentonvilleLocation.key), PENTONVILLE)

    val courtBookingRequest = courtBookingRequest(
      courtCode = "DRBYMC",
      prisonerNumber = "A1111AA",
      prisonCode = PENTONVILLE,
      location = pentonvilleLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    val id = webTestClient.createBooking(courtBookingRequest, COURT_USER)

    // Do the availability check for a booking at the same time as the existing booking above
    val availabilityRequest = AvailabilityRequest(
      bookingType = BookingType.COURT,
      courtOrProbationCode = "DRBYMC",
      prisonCode = PENTONVILLE,
      date = LocalDate.now().plusDays(1),
      mainAppointment = LocationAndInterval(
        pentonvilleLocation.key,
        Interval(
          start = LocalTime.of(12, 0),
          end = LocalTime.of(12, 30),
        ),
      ),
      vlbIdToExclude = id,
    )

    val availabilityResponse = webTestClient.availabilityCheck(availabilityRequest)

    assertThat(availabilityResponse).isNotNull
    with(availabilityResponse) {
      assertThat(availabilityOk).isTrue()
      assertThat(alternatives).hasSize(0)
    }
  }

  @Test
  fun `should confirm availability for a probation booking when the prison room is free`() {
    videoBookingRepository.findAll() hasSize 0

    val availabilityRequest = AvailabilityRequest(
      bookingType = BookingType.PROBATION,
      courtOrProbationCode = "BLKPPP",
      prisonCode = PENTONVILLE,
      date = LocalDate.now().plusDays(1),
      mainAppointment = LocationAndInterval(
        pentonvilleLocation.key,
        Interval(
          start = LocalTime.of(12, 0),
          end = LocalTime.of(12, 30),
        ),
      ),
    )

    val availabilityResponse = webTestClient.availabilityCheck(availabilityRequest)

    assertThat(availabilityResponse).isNotNull
    with(availabilityResponse) {
      assertThat(availabilityOk).isTrue()
      assertThat(alternatives).isEmpty()
    }
  }

  @Test
  fun `should return alternatives for a probation booking when the prison room is already occupied`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("A1111AA", PENTONVILLE)
    locationsInsidePrisonApi().stubGetLocationByKey(pentonvilleLocation.key, PENTONVILLE)

    val probationBookingRequest = probationBookingRequest(
      probationTeamCode = "BLKPPP",
      probationMeetingType = ProbationMeetingType.PSR,
      videoLinkUrl = "https://probation.videolink.com",
      prisonCode = PENTONVILLE,
      prisonerNumber = "A1111AA",
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(9, 30),
      appointmentType = AppointmentType.VLB_PROBATION,
      location = pentonvilleLocation,
    )

    webTestClient.createBooking(probationBookingRequest, PROBATION_USER)

    videoBookingRepository.findAll() hasSize 1

    // Check for availability of the times just created i.e. an overlapping appointment
    val availabilityRequest = AvailabilityRequest(
      bookingType = BookingType.PROBATION,
      courtOrProbationCode = "BLKPPP",
      prisonCode = PENTONVILLE,
      date = LocalDate.now().plusDays(1),
      mainAppointment = LocationAndInterval(
        pentonvilleLocation.key,
        Interval(
          start = LocalTime.of(9, 0),
          end = LocalTime.of(9, 30),
        ),
      ),
    )

    val availabilityResponse = webTestClient.availabilityCheck(availabilityRequest)

    assertThat(availabilityResponse).isNotNull
    with(availabilityResponse) {
      assertThat(availabilityOk).isFalse()
      assertThat(alternatives).hasSize(3)
    }
  }

  private fun WebTestClient.availabilityCheck(request: AvailabilityRequest) =
    this
      .post()
      .uri("/availability")
      .bodyValue(request)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(AvailabilityResponse::class.java)
      .returnResult().responseBody!!
}

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import java.time.LocalDate
import java.time.LocalTime

class VideoLinkBookingIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var videoBookingRepository: VideoBookingRepository

  @Autowired
  private lateinit var prisonAppointmentRepository: PrisonAppointmentRepository

  @Test
  fun `should create a court booking`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)

    val courtBookingRequest = courtBookingRequest(
      prisonerNumber = "123456",
      prisonCode = BIRMINGHAM,
      locationSuffix = "ABCDEDFG",
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest)

    val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    with(persistedBooking) {
      videoBookingId isEqualTo bookingId
      bookingType isEqualTo "COURT"
      court?.courtId isEqualTo courtBookingRequest.courtId
      hearingType isEqualTo courtBookingRequest.courtHearingType?.name
      videoUrl isEqualTo courtBookingRequest.videoLinkUrl
    }

    with(prisonAppointmentRepository.findByVideoBooking(persistedBooking).single()) {
      videoBooking isEqualTo persistedBooking
      prisonCode isEqualTo BIRMINGHAM
      prisonerNumber isEqualTo "123456"
      appointmentType isEqualTo AppointmentType.VLB_COURT_MAIN.name
      appointmentDate isEqualTo LocalDate.now().plusDays(1)
      prisonLocKey isEqualTo "$BIRMINGHAM-ABCDEDFG"
      startTime isEqualTo LocalTime.of(12, 0)
      endTime isEqualTo LocalTime.of(12, 30)
      createdBy isEqualTo "TBD"
    }
  }

  @Test
  fun `should fail to create a clashing court booking`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)

    val courtBookingRequest = courtBookingRequest(
      prisonerNumber = "123456",
      prisonCode = BIRMINGHAM,
      locationSuffix = "A-1-001",
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
    )

    webTestClient.createBooking(courtBookingRequest)

    val clashingBookingRequest = courtBookingRequest(
      prisonerNumber = "123456",
      prisonCode = BIRMINGHAM,
      locationSuffix = "A-1-001",
      startTime = LocalTime.of(12, 15),
      endTime = LocalTime.of(12, 40),
    )

    val error = webTestClient.post()
      .uri("/video-link-booking")
      .bodyValue(clashingBookingRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().is4xxClientError
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    with(error) {
      status isEqualTo 400
      userMessage isEqualTo "Exception: One or more requested court appointments overlaps with an existing appointment at location $BIRMINGHAM-A-1-001"
      developerMessage isEqualTo "One or more requested court appointments overlaps with an existing appointment at location $BIRMINGHAM-A-1-001"
    }
  }

  @Test
  fun `should create a probation booking`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)

    val probationBookingRequest = probationBookingRequest(
      probationTeamId = 1,
      probationMeetingType = ProbationMeetingType.PSR,
      videoLinkUrl = "https://probation.videolink.com",
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(9, 30),
      appointmentType = AppointmentType.VLB_PROBATION,
      locationSuffix = "ABCDEDFG",
    )

    val bookingId = webTestClient.createBooking(probationBookingRequest)

    val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    with(persistedBooking) {
      videoBookingId isEqualTo bookingId
      bookingType isEqualTo "PROBATION"
      probationTeam?.probationTeamId isEqualTo 1
      probationMeetingType isEqualTo ProbationMeetingType.PSR.name
      videoUrl isEqualTo "https://probation.videolink.com"
      createdBy isEqualTo "TBD"
    }

    with(prisonAppointmentRepository.findByVideoBooking(persistedBooking).single()) {
      videoBooking isEqualTo persistedBooking
      prisonCode isEqualTo BIRMINGHAM
      prisonerNumber isEqualTo "123456"
      appointmentType isEqualTo AppointmentType.VLB_PROBATION.name
      appointmentDate isEqualTo LocalDate.now().plusDays(1)
      prisonLocKey isEqualTo "$BIRMINGHAM-ABCDEDFG"
      startTime isEqualTo LocalTime.of(9, 0)
      endTime isEqualTo LocalTime.of(9, 30)
      createdBy isEqualTo "TBD"
    }
  }

  @Test
  fun `should create two probation bookings in same location at different times`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)

    val probationBookingRequest = probationBookingRequest(
      probationTeamId = 1,
      probationMeetingType = ProbationMeetingType.PSR,
      videoLinkUrl = "https://probation.videolink.com",
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(9, 30),
      appointmentType = AppointmentType.VLB_PROBATION,
      locationSuffix = "ABCDEDFG",
    )

    val bookingId = webTestClient.createBooking(probationBookingRequest)

    videoBookingRepository.findById(bookingId).orElseThrow()

    prisonSearchApi().stubGetPrisoner("78910", BIRMINGHAM)

    val secondProbationBookingRequest = probationBookingRequest(
      probationTeamId = 1,
      probationMeetingType = ProbationMeetingType.PSR,
      videoLinkUrl = "https://probation.videolink.com",
      prisonCode = BIRMINGHAM,
      prisonerNumber = "78910",
      startTime = LocalTime.of(9, 30),
      endTime = LocalTime.of(10, 30),
      appointmentType = AppointmentType.VLB_PROBATION,
      locationSuffix = "ABCDEDFG",
    )

    val secondBookingId = webTestClient.createBooking(secondProbationBookingRequest)

    videoBookingRepository.findById(secondBookingId).orElseThrow()

    videoBookingRepository.findAll().map { it.videoBookingId } containsExactlyInAnyOrder listOf(bookingId, secondBookingId)
  }

  @Test
  fun `should fail to create a clashing probation booking`() {
    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)

    val probationBookingRequest = probationBookingRequest(
      probationTeamId = 1,
      probationMeetingType = ProbationMeetingType.PSR,
      videoLinkUrl = "https://probation.videolink.com",
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(9, 30),
      appointmentType = AppointmentType.VLB_PROBATION,
      locationSuffix = "ABCDEDFG",
    )

    webTestClient.createBooking(probationBookingRequest)

    prisonSearchApi().stubGetPrisoner("789012", BIRMINGHAM)

    val clashingBookingRequest = probationBookingRequest(
      probationTeamId = 1,
      probationMeetingType = ProbationMeetingType.PSR,
      videoLinkUrl = "https://probation.videolink.com",
      prisonCode = BIRMINGHAM,
      prisonerNumber = "789012",
      startTime = LocalTime.of(8, 0),
      endTime = LocalTime.of(9, 30),
      appointmentType = AppointmentType.VLB_PROBATION,
      locationSuffix = "ABCDEDFG",
    )

    val error = webTestClient.post()
      .uri("/video-link-booking")
      .bodyValue(clashingBookingRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().is4xxClientError
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    with(error) {
      status isEqualTo 400
      userMessage isEqualTo "Exception: Requested probation appointment overlaps with an existing appointment at location $BIRMINGHAM-ABCDEDFG"
      developerMessage isEqualTo "Requested probation appointment overlaps with an existing appointment at location $BIRMINGHAM-ABCDEDFG"
    }
  }

  private fun WebTestClient.createBooking(request: CreateVideoBookingRequest) =
    this
      .post()
      .uri("/video-link-booking")
      .bodyValue(request)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Long::class.java)
      .returnResult().responseBody!!
}

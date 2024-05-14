package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
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
    videoBookingRepository.findAll().filter { it.bookingType == "COURT" } hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", "MDI")

    val courtBookingRequest = courtBookingRequest(
      prisonerNumber = "123456",
      prisonCode = "MDI",
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
      prisonCode isEqualTo "MDI"
      prisonerNumber isEqualTo "123456"
      appointmentType isEqualTo AppointmentType.HEARING.name
      appointmentDate isEqualTo LocalDate.now().plusDays(1)
      prisonLocKey isEqualTo "MDI-ABCDEDFG"
      startTime isEqualTo LocalTime.of(12, 0)
      endTime isEqualTo LocalTime.of(12, 30)
      createdBy isEqualTo "TBD"
    }
  }

  @Test
  fun `should create a probation booking`() {
    videoBookingRepository.findAll().filter { it.bookingType == "PROBATION" } hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", "MDI")

    val probationBookingRequest = probationBookingRequest(
      probationTeamId = 1,
      probationMeetingType = ProbationMeetingType.PSR,
      videoLinkUrl = "https://probation.videolink.com",
      prisonCode = "MDI",
      prisonerNumber = "123456",
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(9, 30),
      appointmentType = AppointmentType.PRE_SENTENCE_REPORT,
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
      prisonCode isEqualTo "MDI"
      prisonerNumber isEqualTo "123456"
      appointmentType isEqualTo AppointmentType.PRE_SENTENCE_REPORT.name
      appointmentDate isEqualTo LocalDate.now().plusDays(1)
      prisonLocKey isEqualTo "MDI-ABCDEDFG"
      startTime isEqualTo LocalTime.of(9, 0)
      endTime isEqualTo LocalTime.of(9, 30)
      createdBy isEqualTo "TBD"
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

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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository

class VideoLinkBookingIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var videoBookingRepository: VideoBookingRepository

  @Test
  fun `should create a court booking`() {
    videoBookingRepository.findAll().filter { it.bookingType == "COURT" } hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", "MDI")

    val courtBookingRequest = courtBookingRequest()

    val bookingId = webTestClient.createBooking(courtBookingRequest)

    with(videoBookingRepository.findById(bookingId).orElseThrow()) {
      videoBookingId isEqualTo bookingId
      bookingType isEqualTo "COURT"
      court?.courtId isEqualTo courtBookingRequest.courtId
      hearingType isEqualTo courtBookingRequest.courtHearingType?.name
      videoUrl isEqualTo courtBookingRequest.videoLinkUrl
    }
  }

  @Test
  fun `should create a probation booking`() {
    videoBookingRepository.findAll().filter { it.bookingType == "PROBATION" } hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", "MDI")

    val probationBookingRequest = probationBookingRequest()

    val bookingId = webTestClient.createBooking(probationBookingRequest)

    with(videoBookingRepository.findById(bookingId).orElseThrow()) {
      videoBookingId isEqualTo bookingId
      bookingType isEqualTo "PROBATION"
      probationTeam?.probationTeamId isEqualTo probationBookingRequest.probationTeamId
      probationMeetingType isEqualTo probationBookingRequest.probationMeetingType?.name
      videoUrl isEqualTo probationBookingRequest.videoLinkUrl
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

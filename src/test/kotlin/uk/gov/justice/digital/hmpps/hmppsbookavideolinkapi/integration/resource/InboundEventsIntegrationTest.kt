package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.StatusCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WERRINGTON
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.werringtonLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.TestContainersIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.TEST_USERNAME
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.TEST_USER_EMAIL
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.ManageExternalAppointmentsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PrisonerReleasedEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.ReleaseInformation
import java.time.LocalTime

class InboundEventsIntegrationTest : TestContainersIntegrationTestBase() {

  @MockBean
  private lateinit var manageExternalAppointmentsService: ManageExternalAppointmentsService

  @Autowired
  private lateinit var videoBookingRepository: VideoBookingRepository

  @Test
  fun `should cancel a video booking on receipt of a permanent release event`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", WERRINGTON)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(werringtonLocation.key), WERRINGTON)
    manageUsersApi().stubGetUserDetails(TEST_USERNAME, "Test Users Name")
    manageUsersApi().stubGetUserEmail(TEST_USERNAME, TEST_USER_EMAIL)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = WERRINGTON,
      location = werringtonLocation,
      date = tomorrow(),
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, TEST_USERNAME)

    videoBookingRepository.findById(bookingId).orElseThrow().statusCode isEqualTo StatusCode.ACTIVE

    receiveEvent(
      PrisonerReleasedEvent(
        ReleaseInformation(
          nomsNumber = "123456",
          prisonId = WERRINGTON,
          reason = "RELEASED",
        ),
      ).also { it.isPermanent() isBool true },
    )

    waitForMessagesOnQueue(4)

    videoBookingRepository.findById(bookingId).orElseThrow().statusCode isEqualTo StatusCode.CANCELLED
  }

  @Test
  fun `should not cancel a video booking on receipt of a temporary release event`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", WERRINGTON)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(werringtonLocation.key), WERRINGTON)
    manageUsersApi().stubGetUserDetails(TEST_USERNAME, "Test Users Name")
    manageUsersApi().stubGetUserEmail(TEST_USERNAME, TEST_USER_EMAIL)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = WERRINGTON,
      location = werringtonLocation,
      date = tomorrow(),
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, TEST_USERNAME)

    videoBookingRepository.findById(bookingId).orElseThrow().statusCode isEqualTo StatusCode.ACTIVE

    receiveEvent(
      PrisonerReleasedEvent(
        ReleaseInformation(
          nomsNumber = "123456",
          prisonId = WERRINGTON,
          reason = "TEMPORARY_ABSENCE_RELEASE",
        ),
      ).also { it.isTemporary() isBool true },
    )

    waitForMessagesOnQueue(3)

    videoBookingRepository.findById(bookingId).orElseThrow().statusCode isEqualTo StatusCode.ACTIVE
  }

  @Sql("classpath:integration-test-data/seed-historic-booking.sql")
  @Test
  fun `should not cancel historic video booking on receipt of a permanent release event`() {
    val historicBooking = videoBookingRepository.findById(-1).orElseThrow()

    prisonSearchApi().stubGetPrisoner("ABCDEF", WERRINGTON)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(werringtonLocation.key), WERRINGTON)
    manageUsersApi().stubGetUserDetails(TEST_USERNAME, "Test Users Name")
    manageUsersApi().stubGetUserEmail(TEST_USERNAME, TEST_USER_EMAIL)

    receiveEvent(
      PrisonerReleasedEvent(
        ReleaseInformation(
          nomsNumber = "ABCDEF",
          prisonId = WERRINGTON,
          reason = "RELEASED",
        ),
      ).also { it.isPermanent() isBool true },
    )

    waitForMessagesOnQueue(1)

    videoBookingRepository.findById(historicBooking.videoBookingId).orElseThrow().statusCode isEqualTo StatusCode.ACTIVE
  }

  private fun WebTestClient.createBooking(
    request: CreateVideoBookingRequest,
    username: String = "booking@creator.com",
  ) =
    this
      .post()
      .uri("/video-link-booking")
      .bodyValue(request)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Long::class.java)
      .returnResult().responseBody!!
}

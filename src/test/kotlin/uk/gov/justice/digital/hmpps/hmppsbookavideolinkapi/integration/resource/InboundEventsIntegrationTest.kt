package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingHistoryAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.InboundEventsListener
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.ManageExternalAppointmentsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.MergeInformation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PrisonerMergedEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PrisonerReleasedEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.ReleaseInformation
import java.time.LocalTime

class InboundEventsIntegrationTest : SqsIntegrationTestBase() {

  @SpyBean
  private lateinit var inboundEventsListener: InboundEventsListener

  @MockBean
  private lateinit var manageExternalAppointmentsService: ManageExternalAppointmentsService

  @Autowired
  private lateinit var videoBookingRepository: VideoBookingRepository

  @Autowired
  private lateinit var prisonAppointmentRepository: PrisonAppointmentRepository

  @Autowired
  private lateinit var bookingHistoryAppointmentRepository: BookingHistoryAppointmentRepository

  @DisabledIfEnvironmentVariable(named = "CIRCLECI", matches = "true")
  @Test
  fun `should cancel a video booking on receipt of a permanent release event`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", WERRINGTON)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(werringtonLocation.key), WERRINGTON)

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

    val bookingId = webTestClient.createBooking(courtBookingRequest)

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

  @DisabledIfEnvironmentVariable(named = "CIRCLECI", matches = "true")
  @Test
  fun `should not cancel a video booking on receipt of a temporary release event`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", WERRINGTON)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(werringtonLocation.key), WERRINGTON)

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

    val bookingId = webTestClient.createBooking(courtBookingRequest)

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

  @DisabledIfEnvironmentVariable(named = "CIRCLECI", matches = "true")
  @Sql("classpath:integration-test-data/seed-historic-booking.sql")
  @Test
  fun `should not cancel historic video booking on receipt of a permanent release event`() {
    val historicBooking = videoBookingRepository.findById(-1).orElseThrow()

    prisonSearchApi().stubGetPrisoner("ABCDEF", WERRINGTON)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(werringtonLocation.key), WERRINGTON)

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

  @Test
  fun `should not attempt to cancel an already cancelled future booking`() {
    prisonSearchApi().stubGetPrisoner("YD1234", WERRINGTON)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(werringtonLocation.key), WERRINGTON)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "YD1234",
      prisonCode = WERRINGTON,
      location = werringtonLocation,
      date = tomorrow(),
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest)

    prisonSearchApi().stubGetPrisoner(prisonNumber = "YD1234", prisonCode = "TRN", lastPrisonCode = WERRINGTON)

    inboundEventsListener.onMessage(
      raw(
        PrisonerReleasedEvent(
          ReleaseInformation(
            nomsNumber = "YD1234",
            prisonId = "TRN",
            reason = "TRANSFERRED",
          ),
        ),
      ),
    )

    videoBookingRepository.findById(bookingId).orElseThrow().statusCode isEqualTo StatusCode.CANCELLED

    assertDoesNotThrow {
      inboundEventsListener.onMessage(
        raw(
          PrisonerReleasedEvent(
            ReleaseInformation(
              nomsNumber = "YD1234",
              prisonId = "TRN",
              reason = "TRANSFERRED",
            ),
          ),
        ),
      )
    }
  }

  @Test
  @Sql("classpath:integration-test-data/seed-prisoner-merge.sql")
  fun `should merge prison appointments and booking appointment history when prison merged`() {
    val existingBooking = videoBookingRepository.findById(-1).orElseThrow()

    prisonAppointmentRepository.findByVideoBooking(existingBooking).single { it.prisonerNumber == "OLD123" }
    bookingHistoryAppointmentRepository.findAll().single { it.prisonerNumber == "OLD123" }

    inboundEventsListener.onMessage(
      raw(
        PrisonerMergedEvent(
          MergeInformation(
            removedNomsNumber = "OLD123",
            nomsNumber = "NEW123",
          ),
        ),
      ),
    )

    prisonAppointmentRepository.findByVideoBooking(existingBooking).single { it.prisonerNumber == "NEW123" }
    bookingHistoryAppointmentRepository.findAll().single { it.prisonerNumber == "NEW123" }
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

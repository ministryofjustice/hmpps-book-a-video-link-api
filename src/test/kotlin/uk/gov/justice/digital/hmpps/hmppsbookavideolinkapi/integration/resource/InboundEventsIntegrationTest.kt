package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.StatusCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsEntriesExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvilleLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingHistoryAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.InboundEventsListener
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.ManageExternalAppointmentsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.MergeInformation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PrisonerMergedEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PrisonerReleasedEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.ReleaseInformation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.PrisonerMergedTelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.TelemetryService
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

  @MockBean
  private lateinit var telemetryService: TelemetryService

  private val telemetryCaptor = argumentCaptor<PrisonerMergedTelemetryEvent>()

  @DisabledIfEnvironmentVariable(named = "CIRCLECI", matches = "true")
  @Test
  fun `should cancel a video booking on receipt of a permanent release event`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", PENTONVILLE)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(pentonvilleLocation.key), PENTONVILLE)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = PENTONVILLE,
      location = pentonvilleLocation,
      date = tomorrow(),
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, COURT_USER)

    videoBookingRepository.findById(bookingId).orElseThrow().statusCode isEqualTo StatusCode.ACTIVE

    receiveEvent(
      PrisonerReleasedEvent(
        ReleaseInformation(
          nomsNumber = "123456",
          prisonId = PENTONVILLE,
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

    prisonSearchApi().stubGetPrisoner("123456", PENTONVILLE)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(pentonvilleLocation.key), PENTONVILLE)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = PENTONVILLE,
      location = pentonvilleLocation,
      date = tomorrow(),
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, COURT_USER)

    videoBookingRepository.findById(bookingId).orElseThrow().statusCode isEqualTo StatusCode.ACTIVE

    receiveEvent(
      PrisonerReleasedEvent(
        ReleaseInformation(
          nomsNumber = "123456",
          prisonId = PENTONVILLE,
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

    prisonSearchApi().stubGetPrisoner("ABCDEF", PENTONVILLE)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(pentonvilleLocation.key), PENTONVILLE)

    receiveEvent(
      PrisonerReleasedEvent(
        ReleaseInformation(
          nomsNumber = "ABCDEF",
          prisonId = PENTONVILLE,
          reason = "RELEASED",
        ),
      ).also { it.isPermanent() isBool true },
    )

    waitForMessagesOnQueue(1)

    videoBookingRepository.findById(historicBooking.videoBookingId).orElseThrow().statusCode isEqualTo StatusCode.ACTIVE
  }

  @Test
  fun `should not attempt to cancel an already cancelled future booking`() {
    prisonSearchApi().stubGetPrisoner("YD1234", PENTONVILLE)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(pentonvilleLocation.key), PENTONVILLE)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "YD1234",
      prisonCode = PENTONVILLE,
      location = pentonvilleLocation,
      date = tomorrow(),
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, COURT_USER)

    prisonSearchApi().stubGetPrisoner(prisonerNumber = "YD1234", prisonCode = "TRN", lastPrisonCode = PENTONVILLE)

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
    val firstBooking = videoBookingRepository.findById(-1).orElseThrow()
    val secondBooking = videoBookingRepository.findById(-2).orElseThrow()

    prisonAppointmentRepository.findByVideoBooking(firstBooking).single { it.prisonerNumber == "OLD123" }
    prisonAppointmentRepository.findByVideoBooking(secondBooking).single { it.prisonerNumber == "OLD123" }
    bookingHistoryAppointmentRepository.countByPrisonerNumber("OLD123") isEqualTo 2

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

    prisonAppointmentRepository.findByVideoBooking(firstBooking).single { it.prisonerNumber == "NEW123" }
    prisonAppointmentRepository.findByVideoBooking(secondBooking).single { it.prisonerNumber == "NEW123" }
    bookingHistoryAppointmentRepository.countByPrisonerNumber("NEW123") isEqualTo 2
    verify(telemetryService).track(telemetryCaptor.capture())

    with(telemetryCaptor.firstValue) {
      properties() containsEntriesExactlyInAnyOrder mapOf(
        "previous_prisoner_number" to "OLD123",
        "new_prisoner_number" to "NEW123",
      )

      metrics() containsEntriesExactlyInAnyOrder mapOf("bookings_updated" to 2.0)
    }
  }
}

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.model.Movement
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TimeSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.RISLEY
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.SERVICE_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsEntriesExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingFacade
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.AppointmentsChangedInformation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.Identifier
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PrisonerAppointmentsChangedEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.PrisonerAppointmentsCancelledByPrisonTelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.TelemetryService
import java.time.LocalDateTime

class PrisonerAppointmentsChangedEventHandlerTest {
  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()
  private val prisonApiClient: PrisonApiClient = mock()
  private val bookingFacade: BookingFacade = mock()
  private val timeSource = TimeSource { LocalDateTime.of(2024, 4, 9, 12, 0) }
  private val videoBooking = courtBooking().withMainCourtPrisonAppointment()
  private val telemetryService: TelemetryService = mock()
  private val telemetryCaptor = argumentCaptor<PrisonerAppointmentsCancelledByPrisonTelemetryEvent>()
  private val handler = PrisonerAppointmentsChangedEventHandler(prisonAppointmentRepository, prisonApiClient, bookingFacade, timeSource, telemetryService)

  @Test
  fun `should transfer prisoner on cancel`() {
    whenever(prisonAppointmentRepository.findActivePrisonerPrisonAppointmentsAfter("123456", timeSource.today(), timeSource.now().toLocalTime())) doReturn videoBooking.appointments()
    whenever(prisonApiClient.getLatestPrisonerMovementOnDate("123456", timeSource.today())) doReturn Movement.MovementType.TRN

    handler.handle(event(true, PENTONVILLE, "123456"))
    verify(bookingFacade).prisonerTransferred(videoBooking.videoBookingId, SERVICE_USER)
    verify(telemetryService).track(telemetryCaptor.capture())

    with(telemetryCaptor.firstValue) {
      properties() containsEntriesExactlyInAnyOrder mapOf("prison_code" to PENTONVILLE, "prisoner_number" to "123456", "reason" to "TRN")
    }
  }

  @Test
  fun `should not transfer prisoner on not cancel`() {
    handler.handle(event(false, PENTONVILLE, "123456"))

    verifyNoInteractions(prisonAppointmentRepository)
    verifyNoInteractions(prisonApiClient)
    verifyNoInteractions(bookingFacade)
    verifyNoInteractions(telemetryService)
  }

  @Test
  fun `should release prisoner on cancel`() {
    whenever(prisonAppointmentRepository.findActivePrisonerPrisonAppointmentsAfter("54321", timeSource.today(), timeSource.now().toLocalTime())) doReturn videoBooking.appointments()
    whenever(prisonApiClient.getLatestPrisonerMovementOnDate("54321", timeSource.today())) doReturn Movement.MovementType.REL

    handler.handle(event(true, RISLEY, "54321"))
    verify(bookingFacade).prisonerReleased(videoBooking.videoBookingId, SERVICE_USER)
    verify(telemetryService).track(telemetryCaptor.capture())

    with(telemetryCaptor.firstValue) {
      properties() containsEntriesExactlyInAnyOrder mapOf("prison_code" to RISLEY, "prisoner_number" to "54321", "reason" to "REL")
    }
  }

  @Test
  fun `should not release prisoner on not cancel`() {
    handler.handle(event(false, PENTONVILLE, "123456"))

    verifyNoInteractions(prisonAppointmentRepository)
    verifyNoInteractions(prisonApiClient)
    verifyNoInteractions(bookingFacade)
    verifyNoInteractions(telemetryService)
  }

  private fun event(cancel: Boolean, prisonCode: String, prisonerNumber: String) = PrisonerAppointmentsChangedEvent(
    personReference = PersonReference(listOf(Identifier("NOMS", prisonerNumber))),
    additionalInformation = AppointmentsChangedInformation(
      action = if (cancel) "YES" else "NO",
      prisonId = prisonCode,
      user = "SOME_USER",
    ),
  )
}

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.model.Movement
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TimeSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.SERVICE_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingFacade
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.AppointmentsChangedInformation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.Identifier
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PrisonerAppointmentsChangedEvent
import java.time.LocalDateTime

class PrisonerAppointmentsChangedEventHandlerTest {
  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()
  private val prisonApiClient: PrisonApiClient = mock()
  private val bookingFacade: BookingFacade = mock()
  private val timeSource = TimeSource { LocalDateTime.of(2024, 4, 9, 12, 0) }
  private val videoBooking = courtBooking().withMainCourtPrisonAppointment()
  private val handler = PrisonerAppointmentsChangedEventHandler(prisonAppointmentRepository, prisonApiClient, bookingFacade, timeSource)

  @Test
  fun `should transfer prisoner on cancel`() {
    whenever(prisonAppointmentRepository.findActivePrisonerPrisonAppointmentsAfter("123456", timeSource.today(), timeSource.now().toLocalTime())) doReturn videoBooking.appointments()
    whenever(prisonApiClient.getLatestPrisonerMovementOnDate("123456", timeSource.today())) doReturn Movement.MovementType.TRN

    handler.handle(event(true))
    verify(bookingFacade).prisonerTransferred(videoBooking.videoBookingId, SERVICE_USER)
  }

  @Test
  fun `should not transfer prisoner on not cancel`() {
    handler.handle(event(false))

    verifyNoInteractions(prisonAppointmentRepository)
    verifyNoInteractions(prisonApiClient)
    verifyNoInteractions(bookingFacade)
  }

  @Test
  fun `should release prisoner on cancel`() {
    whenever(prisonAppointmentRepository.findActivePrisonerPrisonAppointmentsAfter("123456", timeSource.today(), timeSource.now().toLocalTime())) doReturn videoBooking.appointments()
    whenever(prisonApiClient.getLatestPrisonerMovementOnDate("123456", timeSource.today())) doReturn Movement.MovementType.REL

    handler.handle(event(true))
    verify(bookingFacade).prisonerReleased(videoBooking.videoBookingId, SERVICE_USER)
  }

  @Test
  fun `should not release prisoner on not cancel`() {
    handler.handle(event(false))

    verifyNoInteractions(prisonAppointmentRepository)
    verifyNoInteractions(prisonApiClient)
    verifyNoInteractions(bookingFacade)
  }

  private fun event(cancel: Boolean) = PrisonerAppointmentsChangedEvent(
    personReference = PersonReference(listOf(Identifier("NOMS", "123456"))),
    additionalInformation = AppointmentsChangedInformation(
      action = if (cancel) "YES" else "NO",
      prisonId = PENTONVILLE,
      user = "SOME_USER",
    ),
  )
}

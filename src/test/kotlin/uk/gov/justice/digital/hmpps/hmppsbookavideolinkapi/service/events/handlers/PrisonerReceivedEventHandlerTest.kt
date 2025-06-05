package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TimeSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingFacade
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PrisonerReceivedEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.ReceivedInformation
import java.time.LocalDateTime

class PrisonerReceivedEventHandlerTest {
  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()
  private val timeSource: TimeSource = TimeSource { LocalDateTime.of(2025, 5, 4, 0, 0) }
  private val bookingFacade: BookingFacade = mock()
  private val bookingOne: VideoBooking = mock { on { videoBookingId } doReturn 1 }
  private val bookingTwo: VideoBooking = mock { on { videoBookingId } doReturn 2 }
  private val appointmentOne: PrisonAppointment = mock {
    on { videoBooking } doReturn bookingOne
    on { prisonCode() } doReturn BIRMINGHAM
  }
  private val appointmentTwo: PrisonAppointment = mock {
    on { videoBooking } doReturn bookingTwo
    on { prisonCode() } doReturn BIRMINGHAM
  }
  private val handler = PrisonerReceivedEventHandler(timeSource, prisonAppointmentRepository, bookingFacade)

  @Test
  fun `should cancel bookings when transferred from Birmingham to Pentonville`() {
    whenever(prisonAppointmentRepository.findActivePrisonerPrisonAppointmentsAfter("123456", timeSource.today(), timeSource.now().toLocalTime())) doReturn listOf(appointmentOne, appointmentTwo)

    handler.handle(PrisonerReceivedEvent(ReceivedInformation("123456", "TRANSFERRED", PENTONVILLE)))

    verify(prisonAppointmentRepository).findActivePrisonerPrisonAppointmentsAfter("123456", timeSource.today(), timeSource.now().toLocalTime())
    verify(bookingFacade).prisonerTransferred(1, UserService.getServiceAsUser())
    verify(bookingFacade).prisonerTransferred(2, UserService.getServiceAsUser())
  }

  @Test
  fun `should not cancel bookings when no matching bookings found`() {
    whenever(prisonAppointmentRepository.findActivePrisonerPrisonAppointmentsAfter("123456", timeSource.today(), timeSource.now().toLocalTime())) doReturn emptyList()

    handler.handle(PrisonerReceivedEvent(ReceivedInformation("123456", "TRANSFERRED", PENTONVILLE)))

    verify(prisonAppointmentRepository).findActivePrisonerPrisonAppointmentsAfter("123456", timeSource.today(), timeSource.now().toLocalTime())
    verifyNoInteractions(bookingFacade)
  }

  @Test
  fun `should not cancel bookings when prison is the same`() {
    whenever(prisonAppointmentRepository.findActivePrisonerPrisonAppointmentsAfter("123456", timeSource.today(), timeSource.now().toLocalTime())) doReturn listOf(appointmentOne, appointmentTwo)

    handler.handle(PrisonerReceivedEvent(ReceivedInformation("123456", "TRANSFERRED", BIRMINGHAM)))

    verify(prisonAppointmentRepository).findActivePrisonerPrisonAppointmentsAfter("123456", timeSource.today(), timeSource.now().toLocalTime())
    verifyNoInteractions(bookingFacade)
  }
}

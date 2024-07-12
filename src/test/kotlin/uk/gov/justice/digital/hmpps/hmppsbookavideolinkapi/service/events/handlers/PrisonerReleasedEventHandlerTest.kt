package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.junit.jupiter.api.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.isOnOrAfter
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingFacade
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService.Companion.getServiceAsUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PrisonerReleasedEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.ReleaseInformation
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class PrisonerReleasedEventHandlerTest {
  private val courtBooking: VideoBooking = mock { on { videoBookingId } doReturn 1 }
  private val courtAppointment1: PrisonAppointment = mock { on { videoBooking } doReturn courtBooking }
  private val courtAppointment2: PrisonAppointment = mock { on { videoBooking } doReturn courtBooking }
  private val probationBooking: VideoBooking = mock { on { videoBookingId } doReturn 2 }
  private val probationAppointment: PrisonAppointment = mock { on { videoBooking } doReturn probationBooking }
  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()
  private val bookingFacade: BookingFacade = mock()
  private val handler = PrisonerReleasedEventHandler(prisonAppointmentRepository, bookingFacade)
  private val dateCaptor = argumentCaptor<LocalDate>()
  private val timeCaptor = argumentCaptor<LocalTime>()
  private val timeNow = LocalTime.now()

  @Test
  fun `should ignore temporary absence release of prisoner`() {
    handler.handle(PrisonerReleasedEvent(ReleaseInformation("123456", "TEMPORARY_ABSENCE_RELEASE", BIRMINGHAM)))
    handler.handle(PrisonerReleasedEvent(ReleaseInformation("123456", "SENT_TO_COURT", BIRMINGHAM)))

    verifyNoInteractions(prisonAppointmentRepository, bookingFacade)
  }

  @Test
  fun `should release single video booking on permanent release of prisoner`() {
    whenever(
      prisonAppointmentRepository.findActivePrisonerPrisonAppointmentsAfter(
        eq("123456"),
        eq(LocalDate.now()),
        argThat { time -> time.isOnOrAfter(timeNow) },
      ),
    ) doReturn listOf(courtAppointment1)
    handler.handle(PrisonerReleasedEvent(ReleaseInformation("123456", "RELEASED", BIRMINGHAM)))

    verify(prisonAppointmentRepository).findActivePrisonerPrisonAppointmentsAfter(eq("123456"), dateCaptor.capture(), timeCaptor.capture())

    dateCaptor.firstValue.atTime(timeCaptor.firstValue) isCloseTo LocalDateTime.now()

    verify(bookingFacade).prisonerReleased(courtBooking.videoBookingId, getServiceAsUser())
  }

  @Test
  fun `should release multiple video bookings on permanent release of prisoner`() {
    whenever(
      prisonAppointmentRepository.findActivePrisonerPrisonAppointmentsAfter(
        eq("123456"),
        eq(LocalDate.now()),
        argThat { time -> time.isOnOrAfter(timeNow) },
      ),
    ) doReturn listOf(courtAppointment1, courtAppointment2, probationAppointment)

    handler.handle(PrisonerReleasedEvent(ReleaseInformation("123456", "RELEASED_TO_HOSPITAL", BIRMINGHAM)))

    verify(prisonAppointmentRepository).findActivePrisonerPrisonAppointmentsAfter(eq("123456"), dateCaptor.capture(), timeCaptor.capture())

    dateCaptor.firstValue.atTime(timeCaptor.firstValue) isCloseTo LocalDateTime.now()

    verify(bookingFacade).prisonerReleased(1, getServiceAsUser())
    verify(bookingFacade).prisonerReleased(2, getServiceAsUser())
  }

  @Test
  fun `should transfer single video booking on permanent release of prisoner`() {
    whenever(
      prisonAppointmentRepository.findActivePrisonerPrisonAppointmentsAfter(
        eq("123456"),
        eq(LocalDate.now()),
        argThat { time -> time.isOnOrAfter(timeNow) },
      ),
    ) doReturn listOf(courtAppointment1)
    handler.handle(PrisonerReleasedEvent(ReleaseInformation("123456", "TRANSFERRED", BIRMINGHAM)))

    verify(prisonAppointmentRepository).findActivePrisonerPrisonAppointmentsAfter(eq("123456"), dateCaptor.capture(), timeCaptor.capture())

    dateCaptor.firstValue.atTime(timeCaptor.firstValue) isCloseTo LocalDateTime.now()

    verify(bookingFacade).prisonerTransferred(courtBooking.videoBookingId, getServiceAsUser())
  }

  @Test
  fun `should transfer multiple video bookings on permanent release of prisoner`() {
    whenever(
      prisonAppointmentRepository.findActivePrisonerPrisonAppointmentsAfter(
        eq("123456"),
        eq(LocalDate.now()),
        argThat { time -> time.isOnOrAfter(timeNow) },
      ),
    ) doReturn listOf(courtAppointment1, courtAppointment2, probationAppointment)

    handler.handle(PrisonerReleasedEvent(ReleaseInformation("123456", "TRANSFERRED", BIRMINGHAM)))

    verify(prisonAppointmentRepository).findActivePrisonerPrisonAppointmentsAfter(eq("123456"), dateCaptor.capture(), timeCaptor.capture())

    dateCaptor.firstValue.atTime(timeCaptor.firstValue) isCloseTo LocalDateTime.now()

    verify(bookingFacade).prisonerTransferred(1, getServiceAsUser())
    verify(bookingFacade).prisonerTransferred(2, getServiceAsUser())
  }
}

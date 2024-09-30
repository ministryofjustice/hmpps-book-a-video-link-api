package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withProbationPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingFacade
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService.Companion.getServiceAsUser
import java.time.LocalDate

class CourtHearingLinkReminderJobTest {

  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()
  private val bookingFacade: BookingFacade = mock()
  private val courtHearingLinkReminderJob: CourtHearingLinkReminderJob = CourtHearingLinkReminderJob(prisonAppointmentRepository, bookingFacade)

  @Test
  fun `should call court hearing link reminder for valid court bookings`() {
    val tomorrow = LocalDate.now().plusDays(1)
    val booking = courtBooking().apply { videoUrl = null }.withMainCourtPrisonAppointment()

    whenever(prisonAppointmentRepository.findAllActivePrisonAppointmentsOnDate(tomorrow)) doReturn booking.appointments()

    courtHearingLinkReminderJob.block()

    verify(bookingFacade).courtHearingLinkReminder(courtBooking(), getServiceAsUser())
  }

  @Test
  fun `should not call court hearing link reminder for bookings with video URL`() {
    val tomorrow = LocalDate.now().plusDays(1)
    val booking = courtBooking().withMainCourtPrisonAppointment()

    whenever(prisonAppointmentRepository.findAllActivePrisonAppointmentsOnDate(tomorrow)) doReturn booking.appointments()

    courtHearingLinkReminderJob.block()

    verify(bookingFacade, never()).courtHearingLinkReminder(any(), any())
  }

  @Test
  fun `should not call court hearing link reminder for non-court bookings`() {
    val tomorrow = LocalDate.now().plusDays(1)
    val booking = probationBooking().withProbationPrisonAppointment()

    whenever(prisonAppointmentRepository.findAllActivePrisonAppointmentsOnDate(tomorrow)) doReturn booking.appointments()

    courtHearingLinkReminderJob.block()

    verify(bookingFacade, never()).courtHearingLinkReminder(any(), any())
  }

  @Test
  fun `should not call court hearing link reminder if court is disabled`() {
    val tomorrow = LocalDate.now().plusDays(1)
    val booking = courtBooking(court = court(enabled = false)).withMainCourtPrisonAppointment()

    whenever(prisonAppointmentRepository.findAllActivePrisonAppointmentsOnDate(tomorrow)) doReturn booking.appointments()

    courtHearingLinkReminderJob.block()

    verify(bookingFacade, never()).courtHearingLinkReminder(any(), any())
  }
}

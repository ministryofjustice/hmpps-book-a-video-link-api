package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingFacade
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService.Companion.getServiceAsUser
import java.time.DayOfWeek
import java.time.LocalDate

class CourtHearingLinkReminderJobTest {

  companion object {
    val MONDAY = LocalDate.of(2024, 12, 2).also { require(it.dayOfWeek == DayOfWeek.MONDAY) { "Not Monday" } }
    val TUESDAY = MONDAY.plusDays(1)
    val WEDNESDAY = TUESDAY.plusDays(1)
    val THURSDAY = WEDNESDAY.plusDays(1)
    val FRIDAY = THURSDAY.plusDays(1)
    val SATURDAY = FRIDAY.plusDays(1)
    val SUNDAY = SATURDAY.plusDays(1)
  }

  private val courtBookingWithoutUrl = courtBooking().apply { videoUrl = null }.withMainCourtPrisonAppointment()
  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()
  private val bookingFacade: BookingFacade = mock()

  @Test
  fun `should call court hearing link reminder on Mondays for valid court bookings`() {
    whenever(prisonAppointmentRepository.findAllActivePrisonAppointmentsOnDate(TUESDAY, "VLB_COURT_MAIN")) doReturn courtBookingWithoutUrl.appointments()
    runJobOn(MONDAY)
    verify(bookingFacade).courtHearingLinkReminder(courtBooking(), getServiceAsUser())
  }

  @Test
  fun `should call court hearing link reminder on Tuesday for valid court bookings`() {
    whenever(prisonAppointmentRepository.findAllActivePrisonAppointmentsOnDate(WEDNESDAY, "VLB_COURT_MAIN")) doReturn courtBookingWithoutUrl.appointments()
    runJobOn(TUESDAY)
    verify(bookingFacade).courtHearingLinkReminder(courtBooking(), getServiceAsUser())
  }

  @Test
  fun `should call court hearing link reminder on Wednesday for valid court bookings`() {
    whenever(prisonAppointmentRepository.findAllActivePrisonAppointmentsOnDate(THURSDAY, "VLB_COURT_MAIN")) doReturn courtBookingWithoutUrl.appointments()
    runJobOn(WEDNESDAY)
    verify(bookingFacade).courtHearingLinkReminder(courtBooking(), getServiceAsUser())
  }

  @Test
  fun `should call court hearing link reminder on Thursday for valid court bookings`() {
    whenever(prisonAppointmentRepository.findAllActivePrisonAppointmentsOnDate(FRIDAY, "VLB_COURT_MAIN")) doReturn courtBookingWithoutUrl.appointments()
    runJobOn(THURSDAY)
    verify(bookingFacade).courtHearingLinkReminder(courtBooking(), getServiceAsUser())
  }

  @Test
  fun `should call court hearing link reminder on Friday for valid court bookings`() {
    whenever(prisonAppointmentRepository.findAllActivePrisonAppointmentsOnDate(FRIDAY.plusDays(3), "VLB_COURT_MAIN")) doReturn courtBookingWithoutUrl.appointments()
    runJobOn(FRIDAY)
    verify(bookingFacade).courtHearingLinkReminder(courtBooking(), getServiceAsUser())
  }

  @Test
  fun `should not call court hearing link reminder on Saturdays`() {
    runJobOn(SATURDAY)
    verifyNoInteractions(bookingFacade)
  }

  @Test
  fun `should call court hearing link reminder on Sunday for valid court bookings`() {
    whenever(prisonAppointmentRepository.findAllActivePrisonAppointmentsOnDate(SUNDAY.plusDays(1), "VLB_COURT_MAIN")) doReturn courtBookingWithoutUrl.appointments()
    runJobOn(SUNDAY)
    verify(bookingFacade).courtHearingLinkReminder(courtBooking(), getServiceAsUser())
  }

  @Test
  fun `should not call court hearing link reminder for bookings with video URL`() {
    val booking = courtBooking().withMainCourtPrisonAppointment()
    whenever(prisonAppointmentRepository.findAllActivePrisonAppointmentsOnDate(TUESDAY, "VLB_COURT_MAIN")) doReturn booking.appointments()
    runJobOn(MONDAY)
    verify(bookingFacade, never()).courtHearingLinkReminder(any(), any())
  }

  @Test
  fun `should not call court hearing link reminder if court is disabled`() {
    val booking = courtBooking(court = court(enabled = false)).withMainCourtPrisonAppointment()
    whenever(prisonAppointmentRepository.findAllActivePrisonAppointmentsOnDate(TUESDAY, "VLB_COURT_MAIN")) doReturn booking.appointments()
    runJobOn(MONDAY)
    verify(bookingFacade, never()).courtHearingLinkReminder(any(), any())
  }

  private fun runJobOn(date: LocalDate) {
    CourtHearingLinkReminderJob(prisonAppointmentRepository, bookingFacade) { date.atStartOfDay() }.runJob()
  }
}

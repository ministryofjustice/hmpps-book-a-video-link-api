package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.jobs

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.SERVICE_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withProbationPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.AdditionalBookingDetailRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingFacade
import java.time.DayOfWeek
import java.time.LocalDate

class ProbationOfficerDetailsReminderJobTest {

  companion object {
    val MONDAY = LocalDate.of(2024, 12, 2).also { require(it.dayOfWeek == DayOfWeek.MONDAY) { "Not Monday" } }
    val TUESDAY = MONDAY.plusDays(1)
    val WEDNESDAY = TUESDAY.plusDays(1)
    val THURSDAY = WEDNESDAY.plusDays(1)
    val FRIDAY = THURSDAY.plusDays(1)
    val SATURDAY = FRIDAY.plusDays(1)
    val SUNDAY = SATURDAY.plusDays(1)
  }

  private val probationBooking = probationBooking().withProbationPrisonAppointment()
  private val additionalBookingDetailRepository: AdditionalBookingDetailRepository = mock()
  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()
  private val bookingFacade: BookingFacade = mock()

  @Test
  fun `should call probation officer details reminder on Mondays for valid probation bookings`() {
    whenever(prisonAppointmentRepository.findAllActivePrisonAppointmentsOnDate(TUESDAY, "VLB_PROBATION")) doReturn probationBooking.appointments()
    runJobOn(MONDAY)
    verify(bookingFacade).sendProbationOfficerDetailsReminder(probationBooking, SERVICE_USER)
  }

  @Test
  fun `should call probation officer details reminder on Tuesday for valid probation bookings`() {
    whenever(prisonAppointmentRepository.findAllActivePrisonAppointmentsOnDate(WEDNESDAY, "VLB_PROBATION")) doReturn probationBooking.appointments()
    runJobOn(TUESDAY)
    verify(bookingFacade).sendProbationOfficerDetailsReminder(probationBooking, SERVICE_USER)
  }

  @Test
  fun `should call probation officer details reminder on Wednesday for valid probation bookings`() {
    whenever(prisonAppointmentRepository.findAllActivePrisonAppointmentsOnDate(THURSDAY, "VLB_PROBATION")) doReturn probationBooking.appointments()
    runJobOn(WEDNESDAY)
    verify(bookingFacade).sendProbationOfficerDetailsReminder(probationBooking, SERVICE_USER)
  }

  @Test
  fun `should call probation officer details reminder on Thursday for valid probation bookings`() {
    whenever(prisonAppointmentRepository.findAllActivePrisonAppointmentsOnDate(FRIDAY, "VLB_PROBATION")) doReturn probationBooking.appointments()
    runJobOn(THURSDAY)
    verify(bookingFacade).sendProbationOfficerDetailsReminder(probationBooking, SERVICE_USER)
  }

  @Test
  fun `should call probation officer details reminder on Friday for valid probation bookings`() {
    whenever(prisonAppointmentRepository.findAllActivePrisonAppointmentsOnDate(FRIDAY.plusDays(3), "VLB_PROBATION")) doReturn probationBooking.appointments()
    runJobOn(FRIDAY)
    verify(bookingFacade).sendProbationOfficerDetailsReminder(probationBooking, SERVICE_USER)
  }

  @Test
  fun `should not call probation officer details reminder on Saturdays`() {
    runJobOn(SATURDAY)
    verifyNoInteractions(bookingFacade)
  }

  @Test
  fun `should call probation officer details reminder on Sunday for valid probation bookings`() {
    whenever(prisonAppointmentRepository.findAllActivePrisonAppointmentsOnDate(SUNDAY.plusDays(1), "VLB_PROBATION")) doReturn probationBooking.appointments()
    runJobOn(SUNDAY)
    verify(bookingFacade).sendProbationOfficerDetailsReminder(probationBooking, SERVICE_USER)
  }

  @Test
  fun `should not call probation officer details reminder for bookings with officer details`() {
    whenever(additionalBookingDetailRepository.existsByVideoBooking(probationBooking)) doReturn true
    whenever(prisonAppointmentRepository.findAllActivePrisonAppointmentsOnDate(TUESDAY, "VLB_PROBATION")) doReturn probationBooking.appointments()
    runJobOn(MONDAY)
    verify(bookingFacade, never()).sendProbationOfficerDetailsReminder(any(), any())
  }

  @Test
  fun `should not call probation officer details reminder if probation team is disabled`() {
    val booking = probationBooking(probationTeam = probationTeam(enabled = false)).withProbationPrisonAppointment()
    whenever(prisonAppointmentRepository.findAllActivePrisonAppointmentsOnDate(TUESDAY, "VLB_PROBATION")) doReturn booking.appointments()
    runJobOn(MONDAY)
    verify(bookingFacade, never()).sendProbationOfficerDetailsReminder(any(), any())
  }

  private fun runJobOn(date: LocalDate) {
    ProbationOfficerDetailsReminderJob(additionalBookingDetailRepository, prisonAppointmentRepository, bookingFacade) { date.atStartOfDay() }.runJob()
  }
}

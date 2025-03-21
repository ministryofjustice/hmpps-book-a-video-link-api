package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistoryAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional
import java.util.UUID

class ManageExternalAppointmentsServiceTest {
  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()
  private val activitiesService: ActivitiesAndAppointmentsService = mock()
  private val prisonService: PrisonService = mock()
  private val birminghamLocation = Location(locationId = 123456, locationType = "VLB", "VIDEO LINK", BIRMINGHAM)
  private val courtBooking = courtBooking()
  private val courtAppointment = appointment(
    booking = courtBooking,
    prisonCode = BIRMINGHAM,
    prisonerNumber = "123456",
    appointmentType = "VLB_COURT_PRE",
    date = LocalDate.of(2100, 1, 1),
    startTime = LocalTime.of(11, 0),
    endTime = LocalTime.of(11, 30),
    locationId = UUID.randomUUID(),
  )
  private val service = ManageExternalAppointmentsService(prisonAppointmentRepository, activitiesService, prisonService)

  @Nested
  @DisplayName("Create appointment")
  inner class CreateAppointment {
    @Test
    fun `should create court appointment via activities client when appointments rolled out`() {
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
      whenever(activitiesService.isAppointmentsRolledOutAt(BIRMINGHAM)) doReturn true
      whenever(activitiesService.findMatchingAppointments(courtAppointment)) doReturn emptyList()

      service.createAppointment(1)

      verify(activitiesService).createAppointment(courtAppointment)
    }

    @Test
    fun `should not create court appointment via activities client when appointment already exists`() {
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
      whenever(activitiesService.isAppointmentsRolledOutAt(BIRMINGHAM)) doReturn true
      whenever(activitiesService.findMatchingAppointments(courtAppointment)) doReturn listOf(99)

      service.createAppointment(1)

      verify(activitiesService, never()).createAppointment(courtAppointment)
    }

    @Test
    fun `should create court appointment via prison api client when appointments not rolled out`() {
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
      whenever(activitiesService.isAppointmentsRolledOutAt(BIRMINGHAM)) doReturn false

      service.createAppointment(1)

      verify(activitiesService, never()).createAppointment(courtAppointment)
      verify(prisonService).createAppointment(courtAppointment)
    }

    @Test
    fun `should not create appointment via prison api client when appointment already exists`() {
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
      whenever(activitiesService.isAppointmentsRolledOutAt(BIRMINGHAM)) doReturn false
      whenever(prisonService.findMatchingAppointments(courtAppointment)) doReturn listOf(birminghamLocation.locationId)

      verify(activitiesService, never()).createAppointment(courtAppointment)
      verifyNoInteractions(prisonService)
    }

    @Test
    fun `should be no-op when appointment not found`() {
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.empty()

      service.createAppointment(1)

      verifyNoInteractions(activitiesService)
      verifyNoInteractions(prisonService)
    }
  }

  @Nested
  @DisplayName("Cancel appointment")
  inner class CancelAppointment {

    @Test
    fun `should cancel court appointment via activities client when appointments rolled out`() {
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
      whenever(activitiesService.isAppointmentsRolledOutAt(courtAppointment.prisonCode())) doReturn true
      whenever(activitiesService.findMatchingAppointments(courtAppointment)) doReturn listOf(99)

      service.cancelCurrentAppointment(1)

      verify(activitiesService).cancelAppointment(99)
    }

    @Test
    fun `should not cancel appointment via activities client when appointments rolled out but matching appointment not found`() {
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
      whenever(activitiesService.isAppointmentsRolledOutAt(courtAppointment.prisonCode())) doReturn true
      whenever(activitiesService.findMatchingAppointments(courtAppointment)) doReturn emptyList()

      service.cancelCurrentAppointment(1)

      verify(activitiesService, never()).cancelAppointment(anyLong(), any())
    }

    @Test
    fun `should cancel court appointment via prison api client when appointments not rolled out`() {
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
      whenever(activitiesService.isAppointmentsRolledOutAt(courtAppointment.prisonCode())) doReturn false
      whenever(prisonService.findMatchingAppointments(courtAppointment)) doReturn listOf(99)

      service.cancelCurrentAppointment(1)

      verify(prisonService).cancelAppointment(99)
    }

    @Test
    fun `should not cancel appointment when prison appointment not found`() {
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.empty()

      service.cancelCurrentAppointment(1)

      verifyNoInteractions(activitiesService)
      verifyNoInteractions(prisonService)
    }

    @Test
    fun `should cancel previous appointment via A&A API when A&A is rolled out`() {
      val bookingHistory = buildFakeBookingHistory(courtBooking, courtAppointment)

      whenever(activitiesService.isAppointmentsRolledOutAt(courtAppointment.prisonCode())) doReturn true
      whenever(activitiesService.findMatchingAppointments(bookingHistory.appointments().single())) doReturn listOf(99)

      service.cancelPreviousAppointment(bookingHistory.appointments().first())

      verify(activitiesService).cancelAppointment(99, true)
      verifyNoInteractions(prisonService)
    }

    @Test
    fun `should cancel previous appointment via Prison API when A&A is not rolled out`() {
      val bookingHistory = buildFakeBookingHistory(courtBooking, courtAppointment)

      whenever(activitiesService.isAppointmentsRolledOutAt(courtAppointment.prisonCode())) doReturn false
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
      whenever(prisonService.findMatchingAppointments(bookingHistory.appointments().single())) doReturn listOf(99)

      service.cancelPreviousAppointment(bookingHistory.appointments().first())

      verify(activitiesService, never()).cancelAppointment(anyLong(), any())
      verify(prisonService).cancelAppointment(99)
    }
  }

  private fun buildFakeBookingHistory(booking: VideoBooking, prisonAppointment: PrisonAppointment): BookingHistory {
    val bookingHistory = BookingHistory(
      bookingHistoryId = 1L,
      videoBookingId = booking.videoBookingId,
      historyType = HistoryType.CREATE,
      courtId = booking.court?.courtId,
      hearingType = booking.hearingType,
      createdBy = booking.createdBy,
    )

    val bookingHistoryAppointment = BookingHistoryAppointment(
      bookingHistoryAppointmentId = 1L,
      prisonCode = prisonAppointment.prisonCode(),
      prisonerNumber = prisonAppointment.prisonerNumber,
      appointmentDate = prisonAppointment.appointmentDate,
      appointmentType = prisonAppointment.appointmentType,
      prisonLocationId = prisonAppointment.prisonLocationId,
      startTime = prisonAppointment.startTime,
      endTime = prisonAppointment.endTime,
      bookingHistory = bookingHistory,
    )

    bookingHistory.addBookingHistoryAppointments(listOf(bookingHistoryAppointment))

    return bookingHistory
  }
}

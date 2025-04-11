package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistoryAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.bookingHistory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingHistoryService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.ManageExternalAppointmentsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.VideoBookingAmendedEvent
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional

class VideoBookingAmendedEventHandlerTest {
  private val videoBookingRepository: VideoBookingRepository = mock()
  private val manageExternalAppointmentsService: ManageExternalAppointmentsService = mock()
  private val bookingHistoryService: BookingHistoryService = mock()

  private val handler = VideoBookingAmendedEventHandler(
    videoBookingRepository,
    bookingHistoryService,
    manageExternalAppointmentsService,
  )

  @Test
  fun `should patch an appointment on the booking on receipt of a BOOKING_AMENDED event`() {
    val booking = courtBooking()
      .addAppointment(
        prison = prison(prisonCode = BIRMINGHAM),
        prisonerNumber = "123456",
        appointmentType = "VLB_COURT_PRE",
        date = LocalDate.of(2100, 1, 1),
        startTime = LocalTime.of(11, 0),
        endTime = LocalTime.of(11, 30),
        locationId = birminghamLocation.id,
      )
      .addAppointment(
        prison = prison(prisonCode = BIRMINGHAM),
        prisonerNumber = "123456",
        appointmentType = "VLB_COURT_MAIN",
        date = LocalDate.of(2100, 1, 1),
        startTime = LocalTime.of(12, 0),
        endTime = LocalTime.of(13, 30),
        locationId = birminghamLocation.id,
      )

    val createHistory = with(bookingHistory(HistoryType.CREATE)) {
      addBookingHistoryAppointments(
        listOf(
          BookingHistoryAppointment(
            bookingHistoryAppointmentId = 1L,
            prisonCode = BIRMINGHAM,
            prisonerNumber = "123456",
            appointmentDate = LocalDate.of(2100, 1, 1),
            appointmentType = "VLB_COURT_PRE",
            prisonLocationId = birminghamLocation.id,
            startTime = LocalTime.of(11, 45),
            endTime = LocalTime.of(12, 0),
            bookingHistory = this,
          ),
          BookingHistoryAppointment(
            bookingHistoryAppointmentId = 2L,
            prisonCode = BIRMINGHAM,
            prisonerNumber = "123456",
            appointmentDate = LocalDate.of(2100, 1, 1),
            appointmentType = "VLB_COURT_MAIN",
            prisonLocationId = birminghamLocation.id,
            startTime = LocalTime.of(12, 0),
            endTime = LocalTime.of(13, 30),
            bookingHistory = this,
          ),
        ),
      )

      this
    }

    whenever(videoBookingRepository.findById(anyLong())) doReturn Optional.of(booking)
    whenever(bookingHistoryService.getByVideoBookingId(anyLong())) doReturn listOf(createHistory, bookingHistory(HistoryType.AMEND))

    handler.handle(VideoBookingAmendedEvent(1))

    verify(manageExternalAppointmentsService).amendAppointment(createHistory.appointments()[0], booking.appointments()[0])
    verify(manageExternalAppointmentsService).amendAppointment(createHistory.appointments()[1], booking.appointments()[1])

    verifyNoMoreInteractions(manageExternalAppointmentsService)
  }

  @Test
  fun `should add an appointment to the booking on receipt of a BOOKING_AMENDED event`() {
    val booking = courtBooking()
      .addAppointment(
        prison = prison(prisonCode = BIRMINGHAM),
        prisonerNumber = "123456",
        appointmentType = "VLB_COURT_PRE",
        date = LocalDate.of(2100, 1, 1),
        startTime = LocalTime.of(11, 0),
        endTime = LocalTime.of(11, 30),
        locationId = birminghamLocation.id,
      )
      .addAppointment(
        prison = prison(prisonCode = BIRMINGHAM),
        prisonerNumber = "123456",
        appointmentType = "VLB_COURT_MAIN",
        date = LocalDate.of(2100, 1, 1),
        startTime = LocalTime.of(12, 0),
        endTime = LocalTime.of(13, 30),
        locationId = birminghamLocation.id,
      )

    val createHistory = with(bookingHistory(HistoryType.CREATE)) {
      addBookingHistoryAppointments(
        listOf(
          BookingHistoryAppointment(
            bookingHistoryAppointmentId = 1L,
            prisonCode = BIRMINGHAM,
            prisonerNumber = "123456",
            appointmentDate = LocalDate.of(2100, 1, 1),
            appointmentType = "VLB_COURT_MAIN",
            prisonLocationId = birminghamLocation.id,
            startTime = LocalTime.of(12, 0),
            endTime = LocalTime.of(13, 30),
            bookingHistory = this,
          ),
        ),
      )

      this
    }

    whenever(videoBookingRepository.findById(anyLong())) doReturn Optional.of(booking)
    whenever(bookingHistoryService.getByVideoBookingId(anyLong())) doReturn listOf(createHistory, bookingHistory(HistoryType.AMEND))

    handler.handle(VideoBookingAmendedEvent(1))

    verify(manageExternalAppointmentsService).amendAppointment(createHistory.appointments()[0], booking.appointments()[1])
    verify(manageExternalAppointmentsService).createAppointment(booking.appointments()[0])

    verifyNoMoreInteractions(manageExternalAppointmentsService)
  }

  @Test
  fun `should remove an appointment to the booking on receipt of a BOOKING_AMENDED event`() {
    val booking = courtBooking()
      .addAppointment(
        prison = prison(prisonCode = BIRMINGHAM),
        prisonerNumber = "123456",
        appointmentType = "VLB_COURT_MAIN",
        date = LocalDate.of(2100, 1, 1),
        startTime = LocalTime.of(12, 0),
        endTime = LocalTime.of(13, 30),
        locationId = birminghamLocation.id,
      )

    val createHistory = with(bookingHistory(HistoryType.CREATE)) {
      addBookingHistoryAppointments(
        listOf(
          BookingHistoryAppointment(
            bookingHistoryAppointmentId = 1L,
            prisonCode = BIRMINGHAM,
            prisonerNumber = "123456",
            appointmentDate = LocalDate.of(2100, 1, 1),
            appointmentType = "VLB_COURT_PRE",
            prisonLocationId = birminghamLocation.id,
            startTime = LocalTime.of(11, 45),
            endTime = LocalTime.of(12, 0),
            bookingHistory = this,
          ),
          BookingHistoryAppointment(
            bookingHistoryAppointmentId = 2L,
            prisonCode = BIRMINGHAM,
            prisonerNumber = "123456",
            appointmentDate = LocalDate.of(2100, 1, 1),
            appointmentType = "VLB_COURT_MAIN",
            prisonLocationId = birminghamLocation.id,
            startTime = LocalTime.of(12, 0),
            endTime = LocalTime.of(13, 30),
            bookingHistory = this,
          ),
        ),
      )

      this
    }

    whenever(videoBookingRepository.findById(anyLong())) doReturn Optional.of(booking)
    whenever(bookingHistoryService.getByVideoBookingId(anyLong())) doReturn listOf(createHistory, bookingHistory(HistoryType.AMEND))

    handler.handle(VideoBookingAmendedEvent(1))

    verify(manageExternalAppointmentsService).amendAppointment(createHistory.appointments()[1], booking.appointments()[0])
    verify(manageExternalAppointmentsService).cancelPreviousAppointment(createHistory.appointments()[0])

    verifyNoMoreInteractions(manageExternalAppointmentsService)
  }
}

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistoryAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.bookingHistory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withPreMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withPreMainPostCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withProbationPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingHistoryService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.ActivitiesAndAppointmentsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.ManageExternalAppointmentsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.VideoBookingAmendedEvent
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional

class VideoBookingAmendedEventHandlerTest {
  private val videoBookingRepository: VideoBookingRepository = mock()
  private val manageExternalAppointmentsService: ManageExternalAppointmentsService = mock()
  private val bookingHistoryService: BookingHistoryService = mock()
  private val activitiesService: ActivitiesAndAppointmentsService = mock()

  private val handler = VideoBookingAmendedEventHandler(
    videoBookingRepository,
    bookingHistoryService,
    manageExternalAppointmentsService,
    activitiesService,
  )

  @Nested
  inner class RolledOutPrisons {
    @BeforeEach
    fun before() {
      whenever(activitiesService.isAppointmentsRolledOutAt(anyString())) doReturn true
    }

    @Test
    fun `should patch a bookings appointments to earlier times on the booking on receipt of a BOOKING_AMENDED event`() {
      val amendedBooking = courtBooking().withPreMainPostCourtPrisonAppointment(
        date = LocalDate.of(2100, 1, 1),
        prisonCode = BIRMINGHAM,
        prisonerNumber = "123456",
        startTime = LocalTime.of(11, 45),
        endTime = LocalTime.of(12, 0),
        location = birminghamLocation,
      )

      val createHistory = bookingHistory(HistoryType.CREATE, booking = amendedBooking).apply {
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
              endTime = LocalTime.of(12, 15),
              bookingHistory = this,
            ),
            BookingHistoryAppointment(
              bookingHistoryAppointmentId = 3L,
              prisonCode = BIRMINGHAM,
              prisonerNumber = "123456",
              appointmentDate = LocalDate.of(2100, 1, 1),
              appointmentType = "VLB_COURT_POST",
              prisonLocationId = birminghamLocation.id,
              startTime = LocalTime.of(12, 15),
              endTime = LocalTime.of(12, 30),
              bookingHistory = this,
            ),
          ),
        )
      }

      whenever(videoBookingRepository.findById(anyLong())) doReturn Optional.of(amendedBooking)
      whenever(bookingHistoryService.getByVideoBookingId(anyLong())) doReturn listOf(createHistory, bookingHistory(HistoryType.AMEND, booking = amendedBooking))

      handler.handle(VideoBookingAmendedEvent(1))

      inOrder(manageExternalAppointmentsService) {
        verify(manageExternalAppointmentsService).amendAppointment(createHistory.appointments()[0], amendedBooking.appointments()[0])
        verify(manageExternalAppointmentsService).amendAppointment(createHistory.appointments()[1], amendedBooking.appointments()[1])
        verify(manageExternalAppointmentsService).amendAppointment(createHistory.appointments()[2], amendedBooking.appointments()[2])
      }

      verifyNoMoreInteractions(manageExternalAppointmentsService)
    }

    @Test
    fun `should patch a bookings appointments to later times on the booking on receipt of a BOOKING_AMENDED event`() {
      val amendedBooking = courtBooking().withPreMainPostCourtPrisonAppointment(
        date = LocalDate.of(2100, 1, 1),
        prisonCode = BIRMINGHAM,
        prisonerNumber = "123456",
        startTime = LocalTime.of(12, 15),
        endTime = LocalTime.of(12, 30),
        location = birminghamLocation,
      )

      val createHistory = bookingHistory(HistoryType.CREATE, booking = amendedBooking).apply {
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
              endTime = LocalTime.of(12, 15),
              bookingHistory = this,
            ),
            BookingHistoryAppointment(
              bookingHistoryAppointmentId = 3L,
              prisonCode = BIRMINGHAM,
              prisonerNumber = "123456",
              appointmentDate = LocalDate.of(2100, 1, 1),
              appointmentType = "VLB_COURT_POST",
              prisonLocationId = birminghamLocation.id,
              startTime = LocalTime.of(12, 15),
              endTime = LocalTime.of(12, 30),
              bookingHistory = this,
            ),
          ),
        )
      }

      whenever(videoBookingRepository.findById(anyLong())) doReturn Optional.of(amendedBooking)
      whenever(bookingHistoryService.getByVideoBookingId(anyLong())) doReturn listOf(createHistory, bookingHistory(HistoryType.AMEND, booking = amendedBooking))

      handler.handle(VideoBookingAmendedEvent(1))

      inOrder(manageExternalAppointmentsService) {
        verify(manageExternalAppointmentsService).amendAppointment(createHistory.appointments()[2], amendedBooking.appointments()[2])
        verify(manageExternalAppointmentsService).amendAppointment(createHistory.appointments()[1], amendedBooking.appointments()[1])
        verify(manageExternalAppointmentsService).amendAppointment(createHistory.appointments()[0], amendedBooking.appointments()[0])
      }

      verifyNoMoreInteractions(manageExternalAppointmentsService)
    }

    @Test
    fun `should be no-op when no change to actual appointment date and times for court booking`() {
      val amendedBooking = courtBooking(comments = "comments").withPreMainPostCourtPrisonAppointment(
        date = LocalDate.of(2100, 1, 1),
        prisonCode = BIRMINGHAM,
        prisonerNumber = "123456",
        startTime = LocalTime.of(12, 0),
        endTime = LocalTime.of(12, 15),
        location = birminghamLocation,
      )

      val createHistory = bookingHistory(HistoryType.CREATE, booking = amendedBooking, comments = "comments").apply {
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
              endTime = LocalTime.of(12, 15),
              bookingHistory = this,
            ),
            BookingHistoryAppointment(
              bookingHistoryAppointmentId = 3L,
              prisonCode = BIRMINGHAM,
              prisonerNumber = "123456",
              appointmentDate = LocalDate.of(2100, 1, 1),
              appointmentType = "VLB_COURT_POST",
              prisonLocationId = birminghamLocation.id,
              startTime = LocalTime.of(12, 15),
              endTime = LocalTime.of(12, 30),
              bookingHistory = this,
            ),
          ),
        )
      }

      whenever(videoBookingRepository.findById(anyLong())) doReturn Optional.of(amendedBooking)
      whenever(bookingHistoryService.getByVideoBookingId(anyLong())) doReturn listOf(createHistory, bookingHistory(HistoryType.AMEND, booking = amendedBooking))

      handler.handle(VideoBookingAmendedEvent(1))

      verifyNoInteractions(manageExternalAppointmentsService)
    }

    @Test
    fun `should add an appointment to the booking on receipt of a BOOKING_AMENDED event`() {
      val amendedBooking = courtBooking().withPreMainCourtPrisonAppointment(
        date = LocalDate.of(2100, 1, 1),
        prisonCode = BIRMINGHAM,
        prisonerNumber = "123456",
        startTime = LocalTime.of(12, 0),
        endTime = LocalTime.of(13, 30),
        location = birminghamLocation,
      )

      val createHistory = bookingHistory(HistoryType.CREATE, booking = amendedBooking).apply {
        addBookingHistoryAppointments(
          listOf(
            BookingHistoryAppointment(
              bookingHistoryAppointmentId = 1L,
              prisonCode = BIRMINGHAM,
              prisonerNumber = "123456",
              appointmentDate = LocalDate.of(2100, 1, 1),
              appointmentType = "VLB_COURT_MAIN",
              prisonLocationId = birminghamLocation.id,
              startTime = LocalTime.of(12, 30),
              endTime = LocalTime.of(13, 30),
              bookingHistory = this,
            ),
          ),
        )
      }

      whenever(videoBookingRepository.findById(anyLong())) doReturn Optional.of(amendedBooking)
      whenever(bookingHistoryService.getByVideoBookingId(anyLong())) doReturn listOf(createHistory, bookingHistory(HistoryType.AMEND, booking = amendedBooking))

      handler.handle(VideoBookingAmendedEvent(1))

      verify(manageExternalAppointmentsService).amendAppointment(createHistory.appointments()[0], amendedBooking.appointments()[1])
      verify(manageExternalAppointmentsService).createAppointment(amendedBooking.appointments()[0])

      verifyNoMoreInteractions(manageExternalAppointmentsService)
    }

    @Test
    fun `should remove an appointment to the booking on receipt of a BOOKING_AMENDED event`() {
      val amendedBooking = courtBooking().withMainCourtPrisonAppointment(
        prisonCode = BIRMINGHAM,
        prisonerNumber = "123456",
        date = LocalDate.of(2100, 1, 1),
        startTime = LocalTime.of(12, 0),
        endTime = LocalTime.of(13, 30),
        location = birminghamLocation,
      )

      val createHistory = bookingHistory(HistoryType.CREATE, booking = amendedBooking).apply {
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
              startTime = LocalTime.of(12, 30),
              endTime = LocalTime.of(13, 30),
              bookingHistory = this,
            ),
          ),
        )
      }

      whenever(videoBookingRepository.findById(anyLong())) doReturn Optional.of(amendedBooking)
      whenever(bookingHistoryService.getByVideoBookingId(anyLong())) doReturn listOf(createHistory, bookingHistory(HistoryType.AMEND, booking = amendedBooking))

      handler.handle(VideoBookingAmendedEvent(1))

      verify(manageExternalAppointmentsService).amendAppointment(createHistory.appointments()[1], amendedBooking.appointments()[0])
      verify(manageExternalAppointmentsService).cancelPreviousAppointment(createHistory.appointments()[0])

      verifyNoMoreInteractions(manageExternalAppointmentsService)
    }

    @Test
    fun `should be no-op when no change to actual appointment date and times for probation booking`() {
      val amendedBooking = probationBooking(comments = "comments").withProbationPrisonAppointment(date = today(), startTime = LocalTime.of(13, 30), endTime = LocalTime.of(14, 30), location = birminghamLocation)
      val history = bookingHistory(HistoryType.CREATE, booking = amendedBooking, comments = "comments").apply {
        addBookingHistoryAppointments(
          listOf(
            BookingHistoryAppointment(
              bookingHistoryAppointmentId = 1L,
              prisonCode = BIRMINGHAM,
              prisonerNumber = "123456",
              appointmentDate = today(),
              appointmentType = "VLB_PROBATION",
              prisonLocationId = birminghamLocation.id,
              startTime = LocalTime.of(13, 30),
              endTime = LocalTime.of(14, 30),
              bookingHistory = this,
            ),
          ),
        )
      }

      whenever(videoBookingRepository.findById(anyLong())) doReturn Optional.of(amendedBooking)
      whenever(bookingHistoryService.getByVideoBookingId(anyLong())) doReturn listOf(history, bookingHistory(HistoryType.AMEND, booking = amendedBooking))

      handler.handle(VideoBookingAmendedEvent(1))
      verifyNoInteractions(manageExternalAppointmentsService)
    }
  }

  @Nested
  inner class NonRolledOutPrisons {
    @BeforeEach
    fun before() {
      whenever(activitiesService.isAppointmentsRolledOutAt(anyString())) doReturn false
    }

    @Test
    fun `should cancel all old appointments first and then create new appointments on receipt of a court BOOKING_AMENDED event`() {
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

      val bookingHistory = bookingHistory(HistoryType.CREATE, booking = booking).apply {
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
      }

      whenever(videoBookingRepository.findById(anyLong())) doReturn Optional.of(booking)
      whenever(bookingHistoryService.getByVideoBookingId(anyLong())) doReturn listOf(bookingHistory, bookingHistory(HistoryType.AMEND, booking = booking))

      handler.handle(VideoBookingAmendedEvent(1))

      inOrder(manageExternalAppointmentsService) {
        verify(manageExternalAppointmentsService, times(2)).cancelPreviousAppointment(any())
        verify(manageExternalAppointmentsService, times(2)).createAppointment(any())
      }
    }

    @AfterEach
    fun after() {
      verify(manageExternalAppointmentsService, never()).amendAppointment(any(), any())
    }
  }
}

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistoryAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.BookingHistoryService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.ManageExternalAppointmentsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.VideoBookingAmendedEvent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional

class VideoBookingAmendedEventHandlerTest {
  private val videoBookingRepository: VideoBookingRepository = mock()
  private val manageExternalAppointmentsService: ManageExternalAppointmentsService = mock()
  private val bookingHistoryService: BookingHistoryService = mock()
  private val outboundEventsService: OutboundEventsService = mock()

  private val handler = VideoBookingAmendedEventHandler(
    videoBookingRepository,
    bookingHistoryService,
    outboundEventsService,
    manageExternalAppointmentsService,
  )

  private val booking = courtBooking()
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

  // For use with the mock to return the booking history details

  private val createHistory = BookingHistory(
    bookingHistoryId = 1L,
    videoBookingId = 1L,
    historyType = HistoryType.CREATE,
    courtId = 1L,
    hearingType = CourtHearingType.TRIBUNAL.name,
    videoUrl = "https://created.video.url",
    comments = "Created comment",
    createdBy = "court user",
    createdTime = LocalDateTime.now().minusDays(2),
  )

  private val amendHistory = BookingHistory(
    bookingHistoryId = 2L,
    videoBookingId = 1L,
    historyType = HistoryType.AMEND,
    courtId = 1L,
    hearingType = CourtHearingType.TRIBUNAL.name,
    videoUrl = "https://edited.video.url",
    comments = "Edited comment",
    createdBy = "court user",
    createdTime = LocalDateTime.now().minusDays(1),
  )

  private val historyList = listOf(
    addBookingHistoryAppointments(createHistory),
    addBookingHistoryAppointments(amendHistory),
  )

  @Test
  fun `should remove and recreate external appointments on receipt of a BOOKING_AMENDED event`() {
    whenever(videoBookingRepository.findById(anyLong())) doReturn Optional.of(booking)
    whenever(bookingHistoryService.getByVideoBookingId(anyLong())) doReturn historyList

    handler.handle(VideoBookingAmendedEvent(1))

    verify(manageExternalAppointmentsService, times(2)).cancelPreviousAppointment(any())

    verify(outboundEventsService, times(2)).send(DomainEventType.APPOINTMENT_CREATED, 0)

    verifyNoMoreInteractions(manageExternalAppointmentsService)
  }

  @Test
  fun `should remove and recreate external appointments when missing amend on receipt of a BOOKING_AMENDED event`() {
    whenever(videoBookingRepository.findById(anyLong())) doReturn Optional.of(booking)
    whenever(bookingHistoryService.getByVideoBookingId(anyLong())) doReturn listOf(createHistory)

    handler.handle(VideoBookingAmendedEvent(1))

    verify(manageExternalAppointmentsService, times(2)).cancelPreviousAppointment(any())

    verify(outboundEventsService, times(2)).send(DomainEventType.APPOINTMENT_CREATED, 0)

    verifyNoMoreInteractions(manageExternalAppointmentsService)
  }

  @Test
  fun `should no-op an unknown booking`() {
    whenever(videoBookingRepository.findById(anyLong())) doReturn Optional.empty()

    handler.handle(VideoBookingAmendedEvent(1))

    verifyNoInteractions(outboundEventsService)
    verifyNoInteractions(manageExternalAppointmentsService)
  }

  private fun addBookingHistoryAppointments(hist: BookingHistory): BookingHistory {
    hist.addBookingHistoryAppointments(
      listOf(
        BookingHistoryAppointment(
          bookingHistoryAppointmentId = 1L,
          prisonCode = BIRMINGHAM,
          prisonerNumber = "123456",
          appointmentDate = LocalDate.of(2100, 1, 1),
          appointmentType = "VLB_COURT_PRE",
          prisonLocationId = birminghamLocation.id,
          startTime = LocalTime.of(11, 0),
          endTime = LocalTime.of(11, 30),
          bookingHistory = hist,
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
          bookingHistory = hist,
        ),
      ),
    )
    return hist
  }
}

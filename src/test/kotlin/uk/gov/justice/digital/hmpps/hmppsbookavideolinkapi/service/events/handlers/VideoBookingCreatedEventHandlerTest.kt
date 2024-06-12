package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.VideoBookingCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.VideoBookingInformation
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional

class VideoBookingCreatedEventHandlerTest {

  private val videoBookingRepository: VideoBookingRepository = mock()
  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val booking = courtBooking()
  private val appointments = listOf(
    appointment(
      booking = booking,
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      appointmentType = "VLB_COURT_PRE",
      date = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      locationKey = "",
    ),
    appointment(
      booking = booking,
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      appointmentType = "VLB_COURT_MAIN",
      date = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(13, 30),
      locationKey = "",
    ),
  )
  private val handler = VideoBookingCreatedEventHandler(videoBookingRepository, prisonAppointmentRepository, outboundEventsService)

  @Test
  fun `should publish appointment created event on receipt of video booking`() {
    whenever(videoBookingRepository.findById(1)) doReturn Optional.of(booking)
    whenever(prisonAppointmentRepository.findByVideoBooking(booking)) doReturn appointments

    handler.handle(VideoBookingCreatedEvent(VideoBookingInformation(1)))

    verify(outboundEventsService, times(2)).send(eq(DomainEventType.APPOINTMENT_CREATED), any())
  }

  @Test
  fun `should no-op receipt of unknown video booking`() {
    whenever(videoBookingRepository.findById(1)) doReturn Optional.empty()
    whenever(prisonAppointmentRepository.findByVideoBooking(booking)) doReturn appointments

    handler.handle(VideoBookingCreatedEvent(VideoBookingInformation(1)))

    verifyNoInteractions(prisonAppointmentRepository)
    verifyNoInteractions(outboundEventsService)
  }
}

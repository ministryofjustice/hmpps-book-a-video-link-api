package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.VideoBookingCreatedEvent
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

class VideoBookingCreatedEventHandlerTest {

  private val videoBookingRepository: VideoBookingRepository = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val booking = courtBooking()
    .addAppointment(
      prison = prison(prisonCode = BIRMINGHAM),
      prisonerNumber = "123456",
      appointmentType = "VLB_COURT_PRE",
      date = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      locationId = "",
    )
    .addAppointment(
      prison = prison(prisonCode = BIRMINGHAM),
      prisonerNumber = "123456",
      appointmentType = "VLB_COURT_MAIN",
      date = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(13, 30),
      locationId = "",
    )

  private val handler = VideoBookingCreatedEventHandler(videoBookingRepository, outboundEventsService)

  @Test
  fun `should publish appointment created event on receipt of video booking`() {
    whenever(videoBookingRepository.findById(1)) doReturn Optional.of(booking)

    handler.handle(VideoBookingCreatedEvent(1))

    verify(outboundEventsService, times(2)).send(eq(DomainEventType.APPOINTMENT_CREATED), any())
    verifyNoMoreInteractions(outboundEventsService)
  }

  @Test
  fun `should no-op receipt of unknown video booking`() {
    whenever(videoBookingRepository.findById(1)) doReturn Optional.empty()

    handler.handle(VideoBookingCreatedEvent(1))

    verifyNoInteractions(outboundEventsService)
  }
}

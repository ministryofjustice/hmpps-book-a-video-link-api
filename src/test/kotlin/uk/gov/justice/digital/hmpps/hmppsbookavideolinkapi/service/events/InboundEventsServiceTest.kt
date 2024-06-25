package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.AppointmentCreatedEventHandler
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.VideoBookingCancelledEventHandler
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.VideoBookingCreatedEventHandler

class InboundEventsServiceTest {

  private val appointmentCreatedEventHandler: AppointmentCreatedEventHandler = mock()
  private val videoBookingCreatedEventHandler: VideoBookingCreatedEventHandler = mock()
  private val videoBookingCancelledEventHandler: VideoBookingCancelledEventHandler = mock()
  private val service = InboundEventsService(
    appointmentCreatedEventHandler,
    videoBookingCreatedEventHandler,
    videoBookingCancelledEventHandler,
  )

  @Test
  fun `should call video booking created handler when for video booking created event`() {
    val event1 = VideoBookingCreatedEvent(1)
    service.process(event1)
    verify(videoBookingCreatedEventHandler).handle(event1)

    val event2 = VideoBookingCreatedEvent(2)
    service.process(event2)
    verify(videoBookingCreatedEventHandler).handle(event2)
  }

  @Test
  fun `should call appointments handler when for appointment created event`() {
    val event1 = AppointmentCreatedEvent(1)
    service.process(event1)
    verify(appointmentCreatedEventHandler).handle(event1)

    val event2 = AppointmentCreatedEvent(2)
    service.process(event2)
    verify(appointmentCreatedEventHandler).handle(event2)
  }

  @Test
  fun `should call video booking cancelled handler when for video booking cancelled event`() {
    val event1 = VideoBookingCancelledEvent(1)
    service.process(event1)
    verify(videoBookingCancelledEventHandler).handle(event1)

    val event2 = VideoBookingCancelledEvent(2)
    service.process(event2)
    verify(videoBookingCancelledEventHandler).handle(event2)
  }
}

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.AppointmentCreatedEventHandler
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.VideoBookingCreatedEventHandler

class InboundEventsServiceTest {

  private val appointmentCreatedEventHandler: AppointmentCreatedEventHandler = mock()
  private val videoBookingCreatedEventHandler: VideoBookingCreatedEventHandler = mock()
  private val service = InboundEventsService(appointmentCreatedEventHandler, videoBookingCreatedEventHandler)

  @Test
  fun `should call video booking handler when for video booking created event`() {
    val event1 = VideoBookingCreatedEvent(VideoBookingInformation(1))
    service.process(event1)
    verify(videoBookingCreatedEventHandler).handle(event1)

    val event2 = VideoBookingCreatedEvent(VideoBookingInformation(2))
    service.process(event2)
    verify(videoBookingCreatedEventHandler).handle(event2)
  }

  @Test
  fun `should call appointments handler when for appointment created event`() {
    val event1 = AppointmentCreatedEvent(AppointmentInformation(1))
    service.process(event1)
    verify(appointmentCreatedEventHandler).handle(event1)

    val event2 = AppointmentCreatedEvent(AppointmentInformation(2))
    service.process(event2)
    verify(appointmentCreatedEventHandler).handle(event2)
  }
}

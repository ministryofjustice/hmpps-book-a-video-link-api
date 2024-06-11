package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class InboundEventsServiceTest {

  private val appointmentsService: ManageExternalAppointmentsService = mock()

  private val service = InboundEventsService(appointmentsService)

  @Test
  fun `should call appointments service for booking created event`() {
    service.process(InboundVideoBookingCreatedEvent(VideoBookingInformation(1)))
    verify(appointmentsService).createAppointments(1)

    service.process(InboundVideoBookingCreatedEvent(VideoBookingInformation(2)))
    verify(appointmentsService).createAppointments(2)
  }
}

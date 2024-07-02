package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import java.time.LocalDateTime

class OutboundEventsServiceImplTest {

  private val eventsPublisher: OutboundEventsPublisher = mock()
  private val service = OutboundEventsServiceImpl(eventsPublisher)
  private val eventCaptor = argumentCaptor<DomainEvent<*>>()

  @Test
  fun `should publish video booking created outbound event`() {
    service.send(DomainEventType.VIDEO_BOOKING_CREATED, 1)

    verify(
      expectedEventType = DomainEventType.VIDEO_BOOKING_CREATED,
      expectedAdditionalInformation = VideoBookingInformation(1),
      expectedDescription = "A new video booking has been created in the book a video link service",
    )
  }

  @Test
  fun `should publish appointment created outbound event`() {
    service.send(DomainEventType.APPOINTMENT_CREATED, 1)

    verify(
      expectedEventType = DomainEventType.APPOINTMENT_CREATED,
      expectedAdditionalInformation = AppointmentInformation(1),
      expectedDescription = "A new prison appointment has been created in the book a video link service",
    )
  }

  @Test
  fun `should publish video booking cancelled outbound event`() {
    service.send(DomainEventType.VIDEO_BOOKING_CANCELLED, 1)

    verify(
      expectedEventType = DomainEventType.VIDEO_BOOKING_CANCELLED,
      expectedAdditionalInformation = VideoBookingInformation(1),
      expectedDescription = "A video booking has been cancelled in the book a video link service",
    )
  }

  @Test
  fun `should not be able to publish prisoner released event`() {
    val error = assertThrows<IllegalArgumentException> {
      service.send(DomainEventType.PRISONER_RELEASED, 1)
    }

    error.message isEqualTo "Unsupported domain event ${DomainEventType.PRISONER_RELEASED}"

    verifyNoInteractions(eventsPublisher)
  }

  private fun verify(
    expectedEventType: DomainEventType,
    expectedAdditionalInformation: AdditionalInformation,
    expectedOccurredAt: LocalDateTime = LocalDateTime.now(),
    expectedDescription: String,
  ) {
    verify(eventsPublisher).send(eventCaptor.capture())

    with(eventCaptor.firstValue) {
      eventType isEqualTo expectedEventType.eventType
      additionalInformation isEqualTo expectedAdditionalInformation
      occurredAt isCloseTo expectedOccurredAt
      description isEqualTo expectedDescription
    }

    verifyNoMoreInteractions(eventsPublisher)
  }
}

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isInstanceOf
import java.util.UUID
import org.mockito.kotlin.check as mockitoCheck

class InboundEventsListenerTest {

  private val mapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

  private val inboundEventsService: InboundEventsService = mock()

  private val eventListener = InboundEventsListener(mapper, inboundEventsService)

  @Test
  fun `should delegate booking created domain event to service`() {
    val event = VideoBookingCreatedEvent(VideoBookingInformation(1))
    val message = message(event)
    val rawMessage = mapper.writeValueAsString(message)

    eventListener.onMessage(rawMessage)

    verify(inboundEventsService).process(
      mockitoCheck {
        it isInstanceOf VideoBookingCreatedEvent::class.java
        (it as VideoBookingCreatedEvent).additionalInformation.videoBookingId isEqualTo 1
      },
    )
  }

  @Test
  fun `should delegate appointment created domain event to service`() {
    val event = AppointmentCreatedEvent(AppointmentInformation(1))
    val message = message(event)
    val rawMessage = mapper.writeValueAsString(message)

    eventListener.onMessage(rawMessage)

    verify(inboundEventsService).process(
      mockitoCheck {
        it isInstanceOf AppointmentCreatedEvent::class.java
        (it as AppointmentCreatedEvent).additionalInformation.appointmentId isEqualTo 1
      },
    )
  }

  private fun message(event: DomainEvent<*>) = Message(
    "Notification",
    mapper.writeValueAsString(event),
    UUID.randomUUID().toString(),
    MessageAttributes(EventType(Type = "String", Value = event.eventType)),
  )
}

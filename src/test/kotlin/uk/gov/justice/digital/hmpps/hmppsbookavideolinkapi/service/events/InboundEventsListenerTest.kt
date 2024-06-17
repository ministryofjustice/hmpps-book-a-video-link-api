package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isInstanceOf
import java.util.UUID

class InboundEventsListenerTest {

  private val mapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

  private val inboundEventsService: InboundEventsService = mock()

  private val featureSwitches: FeatureSwitches = mock()

  private val eventListener = InboundEventsListener(featureSwitches, mapper, inboundEventsService)

  @Test
  fun `should delegate booking created domain event to service when SNS feature is enabled`() {
    whenever(featureSwitches.isEnabled(Feature.SNS_ENABLED)) doReturn true

    val event = VideoBookingCreatedEvent(VideoBookingInformation(1))
    val message = message(event)
    val rawMessage = mapper.writeValueAsString(message)

    eventListener.onMessage(rawMessage)

    verify(inboundEventsService).process(
      org.mockito.kotlin.check {
        it isInstanceOf VideoBookingCreatedEvent::class.java
        (it as VideoBookingCreatedEvent).additionalInformation.videoBookingId isEqualTo 1
      },
    )
  }

  @Test
  fun `should delegate appointment created domain event to service when SNS feature is enabled`() {
    whenever(featureSwitches.isEnabled(Feature.SNS_ENABLED)) doReturn true

    val event = AppointmentCreatedEvent(AppointmentInformation(1))
    val message = message(event)
    val rawMessage = mapper.writeValueAsString(message)

    eventListener.onMessage(rawMessage)

    verify(inboundEventsService).process(
      org.mockito.kotlin.check {
        it isInstanceOf AppointmentCreatedEvent::class.java
        (it as AppointmentCreatedEvent).additionalInformation.appointmentId isEqualTo 1
      },
    )
  }

  @Test
  fun `should not delegate booking created domain event to service when SNS feature is not enabled`() {
    whenever(featureSwitches.isEnabled(Feature.SNS_ENABLED)) doReturn false

    val event = AppointmentCreatedEvent(AppointmentInformation(1))
    val message = message(event)
    val rawMessage = mapper.writeValueAsString(message)

    eventListener.onMessage(rawMessage)

    verifyNoInteractions(inboundEventsService)
  }

  private fun message(event: DomainEvent<*>) =
    Message(
      "Notification",
      mapper.writeValueAsString(event),
      UUID.randomUUID().toString(),
      MessageAttributes(EventType("String", event.eventType)),
    )
}

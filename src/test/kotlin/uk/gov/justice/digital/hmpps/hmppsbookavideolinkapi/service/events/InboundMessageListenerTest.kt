package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.FeatureSwitches
import java.util.UUID

class InboundMessageListenerTest {

  private val mapper: ObjectMapper = ObjectMapper().registerKotlinModule().apply {
    this.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  }

  private val inboundEventsService: InboundEventsService = mock()

  private val featureSwitches: FeatureSwitches = mock()

  private val eventListener = InboundMessageListener(featureSwitches, mapper, inboundEventsService)

  @Test
  fun `should delegate booking created domain event to service when SNS feature is enabled`() {
    whenever(featureSwitches.isEnabled(Feature.SNS_ENABLED)) doReturn true

    val event = InboundVideoBookingCreatedEvent(VideoBookingInformation(1))
    val message = message(event)
    val rawMessage = mapper.writeValueAsString(message)

    eventListener.onMessage(rawMessage)

    verify(inboundEventsService).process(event)
  }

  @Test
  fun `should not delegate booking created domain event to service when SNS feature is not enabled`() {
    whenever(featureSwitches.isEnabled(Feature.SNS_ENABLED)) doReturn false

    val event = InboundVideoBookingCreatedEvent(VideoBookingInformation(1))
    val message = message(event)
    val rawMessage = mapper.writeValueAsString(message)

    eventListener.onMessage(rawMessage)

    verifyNoInteractions(inboundEventsService)
  }

  private fun message(event: InboundDomainEvent) =
    Message(
      "Notification",
      mapper.writeValueAsString(event),
      UUID.randomUUID().toString(),
      MessageAttributes(EventType("String", event.eventType)),
    )
}

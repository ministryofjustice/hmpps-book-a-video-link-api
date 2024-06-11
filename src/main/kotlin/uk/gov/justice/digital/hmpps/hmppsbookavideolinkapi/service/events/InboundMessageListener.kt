package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.FeatureSwitches

@Component
class InboundMessageListener(
  private val features: FeatureSwitches,
  private val mapper: ObjectMapper,
  private val inboundEventsService: InboundEventsService,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun onMessage(rawMessage: String) {
    if (features.isEnabled(Feature.SNS_ENABLED)) {
      val message: Message = mapper.readValue(rawMessage)

      when (message.Type) {
        "Notification" -> {
          mapper.readValue<InboundDomainEvent>(message.Message).let { domainEvent ->
            domainEvent.toInboundEventType()?.let { inboundEventType ->
              runCatching {
                inboundEventsService.process(inboundEventType.toInboundEvent(mapper, message.Message))
              }.onFailure {
                log.error("Error processing message ${message.MessageId}", it)
                throw it
              }
            } ?: log.info("Unrecognised event ${domainEvent.eventType}")
          }
        }

        else -> log.info("Unrecognised message type: ${message.Type}")
      }
    }
  }
}

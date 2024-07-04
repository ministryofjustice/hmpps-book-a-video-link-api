package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.module.kotlin.readValue
import io.awspring.cloud.sqs.annotation.SqsListener
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.FeatureSwitches

@Profile("!local")
@Component
class InboundEventsListener(
  private val features: FeatureSwitches,
  private val mapper: ObjectMapper,
  private val inboundEventsService: InboundEventsService,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @SqsListener("bvls", factory = "hmppsQueueContainerFactoryProxy")
  @WithSpan(value = "farsight-devs-book_a_video_link_queue", kind = SpanKind.SERVER)
  fun onMessage(rawMessage: String) {
    if (features.isEnabled(Feature.SNS_ENABLED)) {
      val message: Message = mapper.readValue(rawMessage)

      when (message.Type) {
        "Notification" -> {
          message.toDomainEventType()?.let { eventType ->
            runCatching {
              inboundEventsService.process(eventType.toInboundEvent(mapper, message.Message))
            }.onFailure {
              log.error("LISTENER: Error processing message ${message.MessageId}", it)
              throw it
            }
          } ?: log.info("LISTENER: Unrecognised event ${message.MessageAttributes.eventType.Value}")
        }
        else -> log.info("LISTENER: Ignoring message, actual message type '${message.Type}' is not a Notification.")
      }
    }
  }
}

@JsonNaming(value = PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class EventType(val Value: String, val Type: String)

data class MessageAttributes(val eventType: EventType)

@JsonNaming(value = PropertyNamingStrategies.UpperCamelCaseStrategy::class)
data class Message(
  val Type: String,
  val Message: String,
  val MessageId: String? = null,
  val MessageAttributes: MessageAttributes,
) {
  fun toDomainEventType() = DomainEventType.entries.singleOrNull { it.eventType == MessageAttributes.eventType.Value }
}

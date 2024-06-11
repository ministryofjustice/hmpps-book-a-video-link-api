package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.FeatureSwitches

@Service
class InboundEventsService(
  private val manageExternalAppointmentsService: ManageExternalAppointmentsService,
) {
  companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun process(event: InboundEvent) {
    when (event) {
      is InboundVideoBookingCreatedEvent -> manageExternalAppointmentsService.createAppointments(event.videoBookingId)
      else -> log.warn("Unsupported event ${event.javaClass.name}")
    }
  }
}

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

interface InboundEvent

data class InboundVideoBookingCreatedEvent(val additionalInformation: VideoBookingInformation) :
  InboundDomainEvent(InboundEventType.VIDEO_BOOKING_CREATED.eventType), InboundEvent {
  val videoBookingId = additionalInformation.videoBookingId
}

data class VideoBookingInformation(val videoBookingId: Long)

enum class InboundEventType(val eventType: String) {
  VIDEO_BOOKING_CREATED("book-a-video-link.video-booking.created") {

    override fun toInboundEvent(mapper: ObjectMapper, message: String) =
      mapper.readValue<InboundVideoBookingCreatedEvent>(message)
  },
  ;

  abstract fun toInboundEvent(mapper: ObjectMapper, message: String): InboundEvent
}

open class InboundDomainEvent(val eventType: String) {
  fun toInboundEventType() = InboundEventType.entries.singleOrNull { it.eventType == eventType }
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
)

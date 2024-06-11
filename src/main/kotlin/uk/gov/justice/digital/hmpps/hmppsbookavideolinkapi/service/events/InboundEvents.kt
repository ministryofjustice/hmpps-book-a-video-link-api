package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.module.kotlin.readValue

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

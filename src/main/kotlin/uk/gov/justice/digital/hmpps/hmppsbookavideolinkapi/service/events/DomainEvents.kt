package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.LocalDateTime

interface Event

data class AppointmentCreatedEvent(val additionalInformation: AppointmentInformation) :
  DomainEvent(DomainEventType.APPOINTMENT_CREATED.eventType), Event {
  val appointmentId = additionalInformation.appointmentId
}

data class AppointmentInformation(val appointmentId: Long) : AdditionalInformation

data class VideoBookingCreatedEvent(val additionalInformation: VideoBookingInformation) :
  DomainEvent(DomainEventType.APPOINTMENT_CREATED.eventType), Event {
  val videoBookingId = additionalInformation.videoBookingId
}

open class DomainEvent(val eventType: String) {
  fun toEventType() = DomainEventType.entries.singleOrNull { it.eventType == eventType }
}

data class VideoBookingInformation(val videoBookingId: Long) : AdditionalInformation

enum class DomainEventType(val eventType: String) {
  VIDEO_BOOKING_CREATED("book-a-video-link.video-booking.created") {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) =
      mapper.readValue<VideoBookingCreatedEvent>(message)

    override fun toOutboundEvent(additionalInformation: AdditionalInformation) =
      OutboundHMPPSDomainEvent(
        eventType = eventType,
        additionalInformation = additionalInformation,
        description = "A new video booking has been created in the book a video link service",
      )
  },
  APPOINTMENT_CREATED("book-a-video-link.appointment.created") {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) =
      mapper.readValue<AppointmentCreatedEvent>(message)

    override fun toOutboundEvent(additionalInformation: AdditionalInformation) =
      OutboundHMPPSDomainEvent(
        eventType = eventType,
        additionalInformation = additionalInformation,
        description = "A new prison appointment has been created in the book a video link service",
      )
  },
  ;

  abstract fun toInboundEvent(mapper: ObjectMapper, message: String): Event

  abstract fun toOutboundEvent(additionalInformation: AdditionalInformation): OutboundHMPPSDomainEvent
}

interface AdditionalInformation

data class OutboundHMPPSDomainEvent(
  val eventType: String,
  val additionalInformation: AdditionalInformation,
  val version: String = "1",
  val description: String,
  val occurredAt: LocalDateTime = LocalDateTime.now(),
)

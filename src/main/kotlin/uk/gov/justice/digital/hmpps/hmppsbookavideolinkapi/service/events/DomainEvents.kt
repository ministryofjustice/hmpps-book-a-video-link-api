package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.LocalDateTime

abstract class DomainEvent<T : AdditionalInformation>(
  eventType: DomainEventType,
  val additionalInformation: T,
) {
  val eventType = eventType.eventType
  val description = eventType.description
  val version: String = "1"
  val occurredAt: LocalDateTime = LocalDateTime.now()

  fun toEventType() = DomainEventType.valueOf(eventType)

  override fun toString() = this::class.simpleName + "(eventType = $eventType, additionalInformation = $additionalInformation)"
}

interface AdditionalInformation

class AppointmentCreatedEvent(additionalInformation: AppointmentInformation) :
  DomainEvent<AppointmentInformation>(DomainEventType.APPOINTMENT_CREATED, additionalInformation)

data class AppointmentInformation(val appointmentId: Long) : AdditionalInformation

class VideoBookingCreatedEvent(additionalInformation: VideoBookingInformation) :
  DomainEvent<VideoBookingInformation>(DomainEventType.VIDEO_BOOKING_CREATED, additionalInformation)

data class VideoBookingInformation(val videoBookingId: Long) : AdditionalInformation

enum class DomainEventType(val eventType: String, val description: String) {
  VIDEO_BOOKING_CREATED(
    "book-a-video-link.video-booking.created",
    "A new video booking has been created in the book a video link service",
  ) {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) =
      mapper.readValue<VideoBookingCreatedEvent>(message)
  },
  APPOINTMENT_CREATED(
    "book-a-video-link.appointment.created",
    "A new prison appointment has been created in the book a video link service",
  ) {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) =
      mapper.readValue<AppointmentCreatedEvent>(message)
  },
  ;

  abstract fun toInboundEvent(mapper: ObjectMapper, message: String): DomainEvent<*>
}

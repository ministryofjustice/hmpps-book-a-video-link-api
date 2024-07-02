package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import com.fasterxml.jackson.annotation.JsonIgnore
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

  override fun toString() =
    this::class.simpleName + "(eventType = $eventType, additionalInformation = $additionalInformation)"
}

interface AdditionalInformation

class AppointmentCreatedEvent(additionalInformation: AppointmentInformation) :
  DomainEvent<AppointmentInformation>(DomainEventType.APPOINTMENT_CREATED, additionalInformation) {
  constructor(id: Long) : this(AppointmentInformation(id))
}

data class AppointmentInformation(val appointmentId: Long) : AdditionalInformation

class VideoBookingCreatedEvent(additionalInformation: VideoBookingInformation) :
  DomainEvent<VideoBookingInformation>(DomainEventType.VIDEO_BOOKING_CREATED, additionalInformation) {
  constructor(id: Long) : this(VideoBookingInformation(id))
}

class VideoBookingCancelledEvent(additionalInformation: VideoBookingInformation) :
  DomainEvent<VideoBookingInformation>(DomainEventType.VIDEO_BOOKING_CANCELLED, additionalInformation) {
  constructor(id: Long) : this(VideoBookingInformation(id))
}

data class VideoBookingInformation(val videoBookingId: Long) : AdditionalInformation

class PrisonerReleasedEvent(additionalInformation: ReleaseInformation) :
  DomainEvent<ReleaseInformation>(DomainEventType.PRISONER_RELEASED, additionalInformation) {

  @JsonIgnore
  fun prisonerNumber() = additionalInformation.nomsNumber

  @JsonIgnore
  fun isTemporary() = listOf("TEMPORARY_ABSENCE_RELEASE", "SENT_TO_COURT").contains(additionalInformation.reason)

  @JsonIgnore
  fun isPermanent() = listOf("RELEASED", "RELEASED_TO_HOSPITAL").contains(additionalInformation.reason)
}

data class ReleaseInformation(val nomsNumber: String, val reason: String, val prisonId: String) : AdditionalInformation

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
  VIDEO_BOOKING_CANCELLED(
    "book-a-video-link.video-booking.cancelled",
    "A video booking has been cancelled in the book a video link service",
  ) {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) =
      mapper.readValue<VideoBookingCancelledEvent>(message)
  },
  PRISONER_RELEASED("prisoner-offender-search.prisoner.released", "") {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) =
      mapper.readValue<PrisonerReleasedEvent>(message)
  },
  ;

  abstract fun toInboundEvent(mapper: ObjectMapper, message: String): DomainEvent<*>
}

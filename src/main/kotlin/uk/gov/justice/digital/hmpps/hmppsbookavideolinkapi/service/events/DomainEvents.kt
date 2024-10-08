package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toOffsetString
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.handlers.MigrateVideoBookingEvent
import java.time.LocalDateTime

abstract class DomainEvent<T : AdditionalInformation>(
  eventType: DomainEventType,
  val additionalInformation: T,
) {
  val eventType = eventType.eventType
  val description = eventType.description
  val version: String = "1"
  val occurredAt: String = LocalDateTime.now().toOffsetString()

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

class VideoBookingAmendedEvent(additionalInformation: VideoBookingInformation) :
  DomainEvent<VideoBookingInformation>(DomainEventType.VIDEO_BOOKING_AMENDED, additionalInformation) {
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
  fun isTransferred() = listOf("TRANSFERRED").contains(additionalInformation.reason)

  @JsonIgnore
  fun isPermanent() = listOf("RELEASED", "RELEASED_TO_HOSPITAL").contains(additionalInformation.reason)
}

data class ReleaseInformation(val nomsNumber: String, val reason: String, val prisonId: String) : AdditionalInformation

class PrisonerMergedEvent(additionalInformation: MergeInformation) :
  DomainEvent<MergeInformation>(DomainEventType.PRISONER_MERGED, additionalInformation) {
  fun replacementPrisonerNumber() = additionalInformation.nomsNumber
  fun removedPrisonerNumber() = additionalInformation.removedNomsNumber
}

data class MergeInformation(val nomsNumber: String, val removedNomsNumber: String) : AdditionalInformation

enum class DomainEventType(val eventType: String, val description: String = "") {
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
  VIDEO_BOOKING_AMENDED(
    "book-a-video-link.video-booking.amended",
    "A video booking has been amended in the book a video link service",
  ) {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) =
      mapper.readValue<VideoBookingAmendedEvent>(message)
  },
  PRISONER_RELEASED("prisoner-offender-search.prisoner.released") {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) =
      mapper.readValue<PrisonerReleasedEvent>(message)
  },
  PRISONER_MERGED("prison-offender-events.prisoner.merged") {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) =
      mapper.readValue<PrisonerMergedEvent>(message)
  },
  MIGRATE_VIDEO_BOOKING("whereabouts-api.videolink.migrate") {
    override fun toInboundEvent(mapper: ObjectMapper, message: String) =
      mapper.readValue<MigrateVideoBookingEvent>(message)
  },
  ;

  abstract fun toInboundEvent(mapper: ObjectMapper, message: String): DomainEvent<*>
}

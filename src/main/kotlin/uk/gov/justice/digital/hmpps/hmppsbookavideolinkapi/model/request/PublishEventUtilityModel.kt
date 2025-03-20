package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType

@Schema(description = "Describes an event to be published to the domain events SNS topic")
data class PublishEventUtilityModel(
  @field:NotNull(message = "Event type must be supplied.")
  val event: Event?,

  @field:NotEmpty(message = "At least one identifier must be supplied")
  @Schema(description = "A list of entity identifiers to be published with the event", example = "[1,2]")
  val identifiers: Set<Long>?,
)

enum class Event {
  APPOINTMENT_CREATED,
  VIDEO_BOOKING_CREATED,
  VIDEO_BOOKING_CANCELLED,
  VIDEO_BOOKING_AMENDED,
  ;

  fun toDomainEvent() = DomainEventType.valueOf(name)
}

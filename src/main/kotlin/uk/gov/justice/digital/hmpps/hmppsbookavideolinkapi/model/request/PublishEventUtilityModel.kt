package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

@Schema(description = "Describes an event to be published to the domain events SNS topic")
data class PublishEventUtilityModel(
  @field:NotEmpty(message = "At least one identifier must be supplied")
  @Schema(description = "A list of entity identifiers to be published with the event", example = "[1,2]")
  val identifiers: List<Long>?,
)

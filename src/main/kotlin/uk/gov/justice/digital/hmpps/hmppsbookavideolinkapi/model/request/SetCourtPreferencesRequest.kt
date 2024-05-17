package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty

@Schema(description = "Describes the list of courts to set as preferences for a user")
data class SetCourtPreferencesRequest(
  @field:Valid
  @field:NotEmpty(message = "At least one court code is required")
  @Schema(description = "The list of court codes to set as the preferences for this username.")
  val courtCodes: List<String>,
)

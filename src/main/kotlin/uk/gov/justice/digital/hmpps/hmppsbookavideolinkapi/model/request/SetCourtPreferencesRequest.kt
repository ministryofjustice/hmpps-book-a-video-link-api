package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

@Schema(description = "Describes the list of courts to set as preferences for a user")
data class SetCourtPreferencesRequest(

  @field:NotNull(message = "The username is a mandatory field.")
  @Schema(description = "The username of the person requesting these court preferences", example = "bob@email.com")
  val username: String,

  @field:Valid
  @field:NotEmpty(message = "At least one court code is required")
  @Schema(description = "The list of court codes to set as the preferences for this username.")
  val courtCodes: List<String>,
)

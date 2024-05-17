package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty

@Schema(description = "Describes the list of probation teams to set as preferences for a user")
data class SetProbationTeamPreferencesRequest(
  @field:Valid
  @field:NotEmpty(message = "At least one probation team code is required")
  @Schema(description = "The list of probation team codes to set as the preferences for this username.")
  val probationTeamCodes: List<String>,
)

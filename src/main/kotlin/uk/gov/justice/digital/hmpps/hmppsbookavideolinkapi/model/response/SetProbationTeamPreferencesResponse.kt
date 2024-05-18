package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes the response from setting the user probation team preferences")
data class SetProbationTeamPreferencesResponse(
  @Schema(description = "The count of probation teams saved as preferences for this user")
  val probationTeamsSaved: Int = 0,
)

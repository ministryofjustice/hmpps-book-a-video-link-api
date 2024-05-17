package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes the response from setting the user court preferences")
data class SetCourtPreferencesResponse(
  @Schema(description = "The count of courts saved as preferences for this user")
  val courtsSaved: Int = 0,
)

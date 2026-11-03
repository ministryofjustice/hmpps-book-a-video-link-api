package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes the response from migrated probation team preferences")
data class MigratedProbationTeamPreferencesResponse(
  @Schema(description = "The count of probation teams migrated from a matching (by email address_ external account")
  val probationTeamsMigrated: Int = 0,
)

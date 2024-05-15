package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes the details of a probation team")
data class ProbationTeam(

  @Schema(description = "An internally-generated unique identifier for this probation team.", example = "12345")
  val probationTeamId: Long,

  @Schema(description = "A short code for this probation team.", example = "NORTHWEST-PCC")
  val code: String,

  @Schema(description = "A fuller description for this probation team", example = "North West Primary Care")
  val description: String,

  @Schema(description = "A boolean value to show whether enabled for video link bookings.", example = "true")
  val enabled: Boolean,

  @Schema(description = "Notes relating to this probation team for opening hours, postal address, main contact.", example = "Free form notes")
  val notes: String?,
)

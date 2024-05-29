package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes the details of a prison")
data class Prison(

  @Schema(description = "An internally-generated unique identifier for this prison.", example = "12345")
  val prisonId: Long,

  @Schema(description = "A short code for this prison.", example = "BMI")
  val code: String,

  @Schema(description = "A fuller description for this prison", example = "HMP Birmingham")
  val name: String,

  @Schema(description = "A boolean value to show whether the prison is enabled for self-service video link bookings by court/probation.", example = "true")
  val enabled: Boolean,

  @Schema(description = "Notes relating to this prison, e.g. number of video-enabled rooms, address.", example = "Free form notes")
  val notes: String?,
)

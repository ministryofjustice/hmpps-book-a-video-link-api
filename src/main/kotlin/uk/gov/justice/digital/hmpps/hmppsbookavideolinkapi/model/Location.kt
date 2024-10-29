package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model

import io.swagger.v3.oas.annotations.media.Schema
import java.util.UUID

data class Location(
  @Schema(description = "The unique location uuid for the location", example = "BE2AEA54-BED1-45ED-B50C-6882D92E367B")
  val id: UUID,

  @Schema(description = "The unique location key for the location", example = "BMI-VIDEOLINK")
  val key: String,

  @Schema(description = "The description for the location, can be null", example = "VIDEO LINK")
  val description: String?,

  @Schema(description = "Flag indicates if the location is enabled, true is enabled and false is disabled.", example = "true")
  val enabled: Boolean,
)

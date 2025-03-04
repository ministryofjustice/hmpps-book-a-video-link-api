package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationUsage

data class CreateDecoratedRoomRequest(
  @field:NotNull(message = "The location usage is mandatory")
  @Schema(description = "The location usage for the location", example = "PROBATION", required = true)
  val locationUsage: LocationUsage?,

  @field:NotNull(message = "The location status is mandatory")
  @Schema(description = "The location usage for the location", example = "INACTIVE", required = true)
  val locationStatus: LocationStatus?,

  @Schema(description = "Court or probation team codes allowed to use the room, can be null", example = "[\"DRBYMC\"]", required = false)
  val allowedParties: Set<String>? = null,

  @Schema(description = "The prison video URL for the location, can be null", example = "HMPS123456", required = false)
  @field:Size(max = 120, message = "Prison video URL should not exceed {max} characters")
  val prisonVideoUrl: String? = null,

  @field:Size(max = 400, message = "Comments should not exceed {max} characters")
  @Schema(description = "Optional comments for the decorated location, can be null", example = "Temporarily unavailable due to ongoing work", required = false)
  val comments: String? = null,
)

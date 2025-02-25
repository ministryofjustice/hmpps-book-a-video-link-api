package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.TimeSlot
import java.time.LocalTime
import java.util.UUID

data class AvailableLocationsResponse(
  val locations: List<AvailableLocation>,
)

data class AvailableLocation(
  val name: String,

  @Schema(description = "The start time in 15 minute slots when the location is available in ISO time format (HH:MI)", example = "12:45")
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(description = "The end time in 15 minute slots when the location is available in ISO time format (HH:MI)", example = "12:45")
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime,

  @Schema(description = "The unique key for the prison location (this can change)", example = "BMI-VIDEOLINK")
  val dpsLocationKey: String,

  @Schema(description = "The unique identifier for the prison location", example = "ef88-efefef-3efggg-3323ddd")
  val dpsLocationId: UUID,

  @Schema(description = "The usage for this location, will be null if no attributes set up for location", example = "PROBATION")
  val usage: LocationUsage?,

  @Schema(description = "The time slot the available location falls into", example = "PM")
  val timeSlot: TimeSlot,
)

enum class LocationUsage {
  COURT,
  PROBATION,
  SHARED,
  SCHEDULE,
}

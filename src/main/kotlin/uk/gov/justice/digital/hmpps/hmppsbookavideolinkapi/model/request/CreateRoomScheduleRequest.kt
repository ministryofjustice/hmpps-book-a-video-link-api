package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationUsage
import java.time.LocalTime

data class CreateRoomScheduleRequest(
  @field:NotNull(message = "The location usage is mandatory")
  @Schema(description = "The location usage for the schedule", example = "PROBATION")
  val locationUsage: LocationUsage?,

  @Schema(description = "Court or probation team codes allowed to use the room", example = "[\"DRBYMC\"]")
  val allowedParties: Set<String> = emptySet(),

  @field:Min(value = 1, message = "Start day of week cannot be less than {min}")
  @field:Max(value = 7, message = "Start day of week cannot be more than {min}")
  @field:NotNull(message = "The start day of week is mandatory")
  val startDayOfWeek: Int?,

  @field:Min(value = 1, message = "End day of week cannot be less than {min}")
  @field:Max(value = 7, message = "End day of week cannot be more than {min}")
  @field:NotNull(message = "The end day of week is mandatory")
  val endDayOfWeek: Int?,

  @field:NotNull(message = "The start time is mandatory")
  @Schema(description = "Start time for the schedule", example = "12:00")
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime?,

  @field:NotNull(message = "The end time mandatory")
  @Schema(description = "End time for the schedule", example = "15:00")
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime?,

  @Schema(description = "Notes related to the schedule", example = "Some notes")
  val notes: String?,
)

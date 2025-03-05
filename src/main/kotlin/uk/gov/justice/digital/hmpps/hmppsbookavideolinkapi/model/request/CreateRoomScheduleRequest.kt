package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationScheduleUsage
import java.time.LocalTime

data class CreateRoomScheduleRequest(
  @field:NotNull(message = "The location usage is mandatory")
  @Schema(description = "The location usage for the schedule", example = "PROBATION", required = true)
  val locationUsage: LocationScheduleUsage?,

  @Schema(description = "Court or probation team codes allowed to use the room", example = "[\"DRBYMC\"]")
  val allowedParties: Set<String>? = null,

  @field:Min(value = 1, message = "Start day of week cannot be less than {min}")
  @field:Max(value = 7, message = "Start day of week cannot be more than {min}")
  @field:NotNull(message = "The start day of week is mandatory.")
  @Schema(description = "The day of the week the schedule starts on. The week starts at 1 for Monday and finishes at 7 for Sunday.", required = true)
  val startDayOfWeek: Int?,

  @field:Min(value = 1, message = "End day of week cannot be less than {min}")
  @field:Max(value = 7, message = "End day of week cannot be more than {min}")
  @field:NotNull(message = "The end day of week is mandatory.")
  @Schema(description = "The day of the week the schedule ends on. The week starts at 1 for Monday and finishes at 7 for Sunday.", required = true)
  val endDayOfWeek: Int?,

  @field:NotNull(message = "The start time is mandatory")
  @Schema(description = "Start time for the schedule", example = "12:00", required = true)
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime?,

  @field:NotNull(message = "The end time mandatory")
  @Schema(description = "End time for the schedule", example = "15:00", required = true)
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime?,

  @Schema(description = "Notes related to the schedule", example = "Some notes")
  val notes: String?,
)

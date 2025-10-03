package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class Location(
  @Schema(description = "The location key for the location (this can change)", example = "BMI-VIDEOLINK")
  val key: String,

  @Schema(description = "The prison code for the location", example = "BMI")
  val prisonCode: String,

  @Schema(description = "The description for the location, can be null", example = "VIDEO LINK")
  val description: String?,

  @Schema(description = "Flag indicates if the location is enabled, true is enabled and false is disabled.", example = "true")
  val enabled: Boolean,

  @Schema(description = "The unique UUID for the prison location", example = "ef88-efefef-3efggg-3323ddd")
  val dpsLocationId: UUID,

  @Schema(description = "Additional location attributes returned if any are requested and available for this location.")
  val extraAttributes: RoomAttributes? = null,
)

@Schema(description = "The additional attributes of a video location")
data class RoomAttributes(
  @Schema(description = "The internal ID for this room attribute", example = "123", required = true)
  val attributeId: Long,

  @Schema(description = "The status of the room (ACTIVE, INACTIVE, TEMPORARILY_BLOCKED)", example = "ACTIVE", required = true)
  val locationStatus: LocationStatus,

  @Schema(description = "An optional message relating to an inactive status", example = "Room damaged")
  val statusMessage: String?,

  @Schema(description = "The preferred usage for this room (COURT, PROBATION, SHARED, BLOCKED, SCHEDULE)", example = "SHARED", required = true)
  val locationUsage: LocationUsage,

  @Schema(description = "Court or probation team codes allowed to use the room (comma-separated list)", example = "[\"YRKMAG\",\"DRBYJS\"]")
  val allowedParties: List<String> = emptyList(),

  @Schema(description = "The video URL to access the equipment in this room", example = "https://prison.video.link/123")
  val prisonVideoUrl: String?,

  @Schema(description = "Notes for these additional attributes", example = "some notes")
  val notes: String?,

  val schedule: List<RoomSchedule> = emptyList(),

  @Schema(description = "The start date which a room is blocked from. Only applies to temporarily blocked rooms.")
  val blockedFrom: LocalDate? = null,

  @Schema(description = "The end date which a room is blocked to. Only applies to temporarily blocked rooms.")
  val blockedTo: LocalDate? = null,
)

@Schema(description = "The additional schedule of usage for a video room")
data class RoomSchedule(
  @Schema(description = "The internal ID for this room schedule", example = "345", required = true)
  val scheduleId: Long,

  @Schema(description = "The day when this time-slot starts", example = "Monday", required = true)
  val startDayOfWeek: DayOfWeek,

  @Schema(description = "The day when this time-slot ends", example = "Friday", required = true)
  val endDayOfWeek: DayOfWeek,

  @Schema(description = "Start time of this slot (24 hr clock, HH:MI)", example = "10:00", required = true)
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(description = "End time of this slot (24 hr clock, HH:MI)", example = "16:00", required = true)
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime,

  @Schema(description = "The usage of this room within this slot (PROBATION, COURT, SHARED, BLOCKED)", example = "SHARED", required = true)
  @Enumerated(EnumType.STRING)
  val locationUsage: LocationScheduleUsage,

  @Schema(description = "Court or probation codes (comma-separated) that can use the room within this slot", example = "[YRKMAG,DRBYJS]")
  val allowedParties: List<String> = emptyList(),
)

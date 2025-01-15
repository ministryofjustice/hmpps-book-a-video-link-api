package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationUsage
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

data class Location(
  @Schema(description = "The unique location key for the location", example = "BMI-VIDEOLINK")
  val key: String,

  @Schema(description = "The description for the location, can be null", example = "VIDEO LINK")
  val description: String?,

  @Schema(description = "Flag indicates if the location is enabled, true is enabled and false is disabled.", example = "true")
  val enabled: Boolean,

  @Schema(description = "Optional additional attributes about the room and its usage.", example = "true")
  val extraAttributes: RoomAttributes? = null,
)

@Schema(description = "The additional attributes of a video location")
data class RoomAttributes(
  @Schema(description = "The status of the room (ACTIVE or INACTIVE)", example = "ACTIVE")
  val locationStatus: LocationStatus,

  @Schema(description = "An optional message relating to an inactive status", example = "Room damaged")
  val statusMessage: String?,

  @Schema(description = "The data that the room might be operational again", example = "22/0/2025")
  val expectedActiveDate: LocalDate?,

  @Schema(description = "The preferred usage for this room (COURT, PROBATION, SHARED, SCHEDULE)", example = "SHARED")
  @Enumerated(EnumType.STRING)
  val locationUsage: LocationUsage,

  @Schema(description = "Court or probation team codes that can use the room (comma-separated)", example = "YRKMAG,DRBYJS")
  val allowedParties: String?,

  @Schema(description = "The video URL to access the equipment in this room", example = "https://prison.video.link/123")
  val prisonVideoUrl: String?,

  @Schema(description = "A schedule for this room. Only present if the locationUsage is SCHEDULE.")
  val schedule: List<RoomSchedule> = emptyList(),
)

@Schema(description = "The additional schedule of usage for a video room")
data class RoomSchedule(
  @Schema(description = "The day when this time-slot starts", example = "Monday")
  val startDayOfWeek: DayOfWeek,

  @Schema(description = "The day when this time-slot ends", example = "Friday")
  val endDayOfWeek: DayOfWeek,

  @Schema(description = "Start time of this slot (24 hr clock, HH:MI)", example = "10:00")
  val startTime: LocalTime,

  @Schema(description = "End time of this slot (24 hr clock, HH:MI)", example = "16:00")
  val endTime: LocalTime,

  @Schema(description = "The usage of this room within this slot (PROBATION, COURT, SHARED)", example = "SHARED")
  @Enumerated(EnumType.STRING)
  val locationUsage: LocationUsage,

  @Schema(description = "Court or probation codes (comma-separated) that can use the room within this slot", example = "YRKMAG,DRBYJS")
  val allowedParties: String?,
)

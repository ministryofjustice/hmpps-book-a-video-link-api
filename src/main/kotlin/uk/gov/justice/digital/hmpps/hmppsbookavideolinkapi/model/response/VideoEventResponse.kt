package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Schema(description = "Response containing booked events occupying video rooms in a prison")
data class VideoEventResponse(

  @Schema(description = "The prison code", example = "MDI")
  val prisonCode: String,

  @Schema(description = "The start date for events retrieved", example = "2022-12-23", required = true)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  val startDate: LocalDate,

  @Schema(description = "The end date for events retrieved", example = "2022-12-23", required = true)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  val endDate: LocalDate,

  @Schema(description = "The optional time slot of the events retrieved", example = "AM")
  val timeSlot: String? = null,

  @Schema(description = "The list of locations and booked events occupying them")
  val locations: List<LocationEvent> = emptyList(),
)

@Schema(description = "A location where events are booked")
data class LocationEvent(

  @Schema(description = "The DPS location UUID where events are booked", example = "a4fe3fef-34fd-4354fde-a12efe")
  val dpsLocationId: UUID?,

  @Schema(description = "The local name for this location", example = "VCC Room 1")
  val localName: String? = null,

  @Schema(description = "The maximum capacity of this room (in persons)", example = "4")
  val capacity: Int? = null,

  @Schema(description = "The list of booked events in this date range and time period")
  val events: List<BookedEvent> = emptyList(),
)

@Schema(description = "A booked event within a single location")
data class BookedEvent(

  @Schema(description = "Event type booked (APPOINTMENT, OFFICIAL_VISIT, COURT, PROBATION)", example = "APPOINTMENT")
  val eventType: String,

  @Schema(description = "The sub-type for this event. e.g. APPOINTMENT - VLOO, COURT - hearing type, PROBATION - meeting type", example = "BAIL")
  val subType: String? = null,

  @Schema(description = "The sub-type description, the reference description for the event subtype", example = "Bail hearing")
  val subTypeDescription: String? = null,

  @Schema(description = "The date of the event", example = "2022-12-23")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  val eventDate: LocalDate,

  @Schema(description = "Start time for the event", example = "10:45")
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(description = "End time for the event", example = "11:45")
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime,

  @Schema(description = "The prisoner this event is booked for", example = "G1234GV")
  val prisonerCode: String,

  @Schema(description = "The event primary key in the remote service (e.g. appointmentId, officialVisitId, videoBookingId)", example = "12345566")
  val eventId: Long? = null,
)

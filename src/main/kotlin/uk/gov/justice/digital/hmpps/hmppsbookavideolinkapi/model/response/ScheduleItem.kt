package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

@Schema(description = "An item on a schedule i.e. prison appointments and their booking details")
data class ScheduleItem(
  @Schema(description = "The internal ID for the video booking", example = "123")
  val videoBookingId: Long,

  @Schema(description = "The internal ID for an appointment related to a booking. It is unique in this list", example = "123")
  val prisonAppointmentId: Long,

  @Schema(description = "The booking type", example = "COURT")
  val bookingType: BookingType,

  @Schema(description = "The booking status", example = "ACTIVE")
  val statusCode: BookingStatus,

  @Schema(description = "The video link URL to attend this event", example = "https://video.link.url")
  val videoUrl: String?,

  @Schema(description = "The comments provided for the booking", example = "Free text comment")
  val bookingComments: String?,

  @Schema(description = "True if the booking was made by a prison user", allowableValues = ["true", "false"], example = "false")
  val createdByPrison: Boolean,

  @Schema(description = "The internal court ID, if this is a court booking", example = "1234")
  val courtId: Long?,

  @Schema(description = "The court code, if this is a court booking", example = "DRBYMC")
  val courtCode: String?,

  @Schema(description = "The court description, if this is a court booking", example = "Derby Magistrates")
  val courtDescription: String?,

  @Schema(description = "The court hearing type code, if this is a court booking", example = "APPEAL")
  val hearingType: CourtHearingType?,

  @Schema(description = "The court hearing type description, if this is a court booking", example = "Appeal hearing")
  val hearingTypeDescription: String?,

  @Schema(description = "The internal probation team ID, if this is a probation booking", example = "1234")
  val probationTeamId: Long?,

  @Schema(description = "The internal probation team code, if this is a probation booking", example = "BLCKPPP")
  val probationTeamCode: String?,

  @Schema(description = "The probation team description, if this is a probation booking", example = "Blackpool PP")
  val probationTeamDescription: String?,

  @Schema(description = "The probation meeting type code, if this is a probation booking", example = "PSR")
  val probationMeetingType: ProbationMeetingType?,

  @Schema(description = "The probation meeting type description, if this is a probation booking", example = "Pre-sentence report")
  val probationMeetingTypeDescription: String?,

  @Schema(description = "The prison code", example = "MDI")
  val prisonCode: String,

  @Schema(description = "The prison name", example = "HMP Moorland")
  val prisonName: String,

  @Schema(description = "The prisoner number (NOMS ID)", example = "A1234AA")
  val prisonerNumber: String,

  @Schema(description = "The appointment type", example = "1234")
  val appointmentType: AppointmentType,

  @Schema(description = "The appointment type description", example = "Court - main hearing")
  val appointmentTypeDescription: String?,

  @Schema(description = "The appointment comments", example = "This is a free text comment")
  val appointmentComments: String?,

  @Schema(
    description = "The location key for the room where the appointment will take place in the prison.",
    example = "MDI-VCC-1",
  )
  val prisonLocKey: String,

  @Schema(
    description = "The location description where the appointment takes place. The localName from the locations-inside-prison service).",
    example = "VCC-crown-conference-room-1",
  )
  val prisonLocDesc: String,

  @Schema(
    description = "The unique UUID for the location where the appointment takes place. The id field from the locations-inside-prison service.",
    example = "a4fe3fef-34fd-4354fde-a12efe",
  )
  val dpsLocationId: UUID,

  @Schema(description = "The date for this appointment ISO format (YYYY-MM-DD)", example = "2024-10-03")
  val appointmentDate: LocalDate,

  @Schema(description = "The start time for the appointment ISO time format (HH:MI)", example = "12:45")
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime,

  @Schema(description = "The end time for the appointment ISO time format (HH:MI)", example = "13:15")
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime,

  @Schema(description = "The timestamp when the booking was created", example = "2024-10-01 14:45")
  val createdTime: LocalDateTime,

  @Schema(description = "The username of the person who created the booking", example = "creator@email.com")
  val createdBy: String,

  @Schema(description = "The timestamp when the booking was last updated", example = "2024-10-02 14:45")
  val updatedTime: LocalDateTime? = null,

  @Schema(description = "The username of the person who updated the booking", example = "amender@email.com")
  val updatedBy: String? = null,

  @Schema(description = "The name of the probation officer if this is a probation booking and present", example = "Jane Doe")
  val probationOfficerName: String? = null,

  @Schema(description = "The email address of the probation officer if this is a probation booking and present", example = "jane.doe@somewhere.com")
  val probationOfficerEmailAddress: String? = null,
)

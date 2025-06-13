package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response

import dev.zacsweers.redacted.annotations.Redacted
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import java.time.LocalDateTime

data class VideoLinkBooking(

  @Schema(description = "The internal ID for this booking", example = "123")
  val videoLinkBookingId: Long,

  @Schema(description = "The status of this booking", example = "ACTIVE")
  val statusCode: BookingStatus,

  @Schema(description = "The booking type", example = "COURT")
  val bookingType: BookingType,

  @Schema(description = "The prisone appointments related to this booking")
  val prisonAppointments: List<PrisonAppointment>,

  @Schema(description = "The court code for booking type COURT, otherwise null", example = "DRBYMC")
  val courtCode: String? = null,

  @Schema(description = "The court description for booking type COURT, otherwise null", example = "Derby Justice Centre")
  val courtDescription: String? = null,

  @Schema(description = "The court hearing type for booking type COURT, otherwise null", example = "APPEAL")
  val courtHearingType: CourtHearingType? = null,

  @Schema(description = "The court hearing type description, for booking type COURT, otherwise null", example = "Appeal hearing")
  val courtHearingTypeDescription: String? = null,

  @Schema(description = "The probation team code for booking type PROBATION, otherwise null", example = "BLKPPP")
  val probationTeamCode: String? = null,

  @Schema(description = "The probation team description for booking type PROBATION, otherwise null", example = "Barnet PPOC")
  val probationTeamDescription: String? = null,

  @Schema(description = "The probation meeting type for booking type PROBATION, otherwise null", example = "PSR")
  val probationMeetingType: ProbationMeetingType? = null,

  @Schema(description = "The probation meeting type description, required for booking type PROBATION", example = "Pre-sentence report")
  val probationMeetingTypeDescription: String? = null,

  @Schema(description = "Free text comments for the video link booking", example = "Waiting to hear on legal representation")
  @Redacted
  val comments: String?,

  @Schema(description = "The video link for the video booking. Must be a valid URL. When this is provided the HMCTS number will be null.", example = "https://video.here.com")
  @Redacted
  val videoLinkUrl: String?,

  @Schema(description = "The HMCTS number for the video booking. When this is provided the video link will be null.", example = "12345678")
  @Redacted
  val hmctsNumber: String?,

  @Schema(description = "The guest PIN to access the video booking.", example = "46385765")
  @Redacted
  val guestPin: String?,

  @Schema(description = "True if the booking was made by a prison user.", example = "false")
  val createdByPrison: Boolean? = false,

  @Schema(description = "Username of the person who created this booking.", example = "creator@email.com")
  val createdBy: String,

  @Schema(description = "Date and time it was originally created.", example = "2024-03-13 11:03")
  val createdAt: LocalDateTime,

  @Schema(description = "Username of the person who last amended this booking.", example = "amender@email.com")
  val amendedBy: String?,

  @Schema(description = "Date and time of the last amendment to this booking.", example = "2024-03-14 14:45")
  val amendedAt: LocalDateTime?,

  @Schema(description = "Additional details for the booking if there are any.")
  val additionalBookingDetails: AdditionalBookingDetails? = null,

  @Schema(
    description = "Private free text notes for the booking.",
    example = "Legal representation details ...",
  )
  @Redacted
  val notesForStaff: String?,

  @Schema(
    description = "Public free text notes for the booking. These notes are visible outside of the service, care should be taken what is entered.",
    example = "Please arrive 10 minutes early",
  )
  @Redacted
  val notesForPrisoners: String?,
)

enum class BookingStatus {
  ACTIVE,
  CANCELLED,
}

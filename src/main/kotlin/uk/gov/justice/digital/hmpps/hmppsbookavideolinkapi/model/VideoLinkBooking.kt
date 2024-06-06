package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import java.time.LocalDateTime

data class VideoLinkBooking(

  @Schema(description = "The internal ID for this booking", example = "123")
  val videoLinkBookingId: Long,

  val status: BookingStatus,

  @Schema(description = "The booking type", example = "COURT")
  val bookingType: BookingType?,

  @Schema(description = "The prisone appointments related to this booking")
  val prisonAppointments: List<PrisonAppointment>,

  @Schema(description = "The court code is needed if booking type is COURT, otherwise null", example = "DRBYMC")
  val courtCode: String? = null,

  @Schema(description = "The court descriptipn for booking types of COURT, otherwise null", example = "Derby Justice Centre")
  val courtDescription: String? = null,

  @Schema(description = "The court hearing type is needed if booking type is COURT, otherwise null", example = "APPEAL")
  val courtHearingType: CourtHearingType? = null,

  @Schema(description = "The court hearing type description, required for booking type COURT", example = "Appeal hearing")
  val courtHearingTypeDescription: String? = null,

  @Schema(description = "The probation team code is needed if booking type is PROBATION, otherwise null", example = "BLKPPP")
  val probationTeamCode: String? = null,

  @Schema(description = "The probation team description, required for booking type PROBATION", example = "Barnet PPOC")
  val probationTeamDescription: String? = null,

  @Schema(description = "The probation meeting type is needed if booking type is PROBATION, otherwise null", example = "PSR")
  val probationMeetingType: ProbationMeetingType? = null,

  @Schema(description = "The probation meeting type description, required for booking type PROBATION", example = "Pre-sentence report")
  val probationMeetingTypeDescription: String? = null,

  @field:Size(max = 400, message = "Comments for the video link booking cannot not exceed {max} characters")
  @Schema(description = "Free text comments for the video link booking", example = "Waiting to hear on legal representation")
  val comments: String?,

  @field:Size(max = 120, message = "The video link should not exceed {max} characters")
  @Schema(description = "The video link for the appointment. Must be a valid URL", example = "https://video.here.com")
  val videoLinkUrl: String?,

  @Schema(description = "Set to true when called by a prison request. Will default to false.", example = "false")
  val createdByPrison: Boolean? = false,

  @Schema(description = "Username of the person who created this booking.", example = "creator@email.com")
  val createdBy: String,

  @Schema(description = "Date and time it was originally created.", example = "2024-03-13 11:03")
  val createdAt: LocalDateTime,

  @Schema(description = "Username of the person who last amended this booking.", example = "amender@email.com")
  val amendedBy: String?,

  @Schema(description = "Date and time of the last amendment to this booking.", example = "2024-03-14 14:45")
  val amendedAt: LocalDateTime?,
)

enum class BookingStatus {
  ACTIVE,
  CANCELLED,
}

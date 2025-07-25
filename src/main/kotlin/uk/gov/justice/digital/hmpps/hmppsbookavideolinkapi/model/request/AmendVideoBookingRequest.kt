package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import com.fasterxml.jackson.annotation.JsonIgnore
import dev.zacsweers.redacted.annotations.Redacted
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

@Schema(
  description =
  """
  Amend an existing court or probation team meeting video link booking.
  """,
)
data class AmendVideoBookingRequest(

  @field:NotNull(message = "The video link booking type is mandatory")
  @Schema(description = "The booking type", example = "COURT")
  val bookingType: BookingType?,

  @field:Valid
  @field:NotEmpty(message = "At least one prisoner must be supplied for a video link booking")
  @Schema(
    description = """
    One or more prisoners associated with the video link booking.
    
    A probation booking should only ever have one prisoner whilst a court booking can have multiple e.g. for co-defendants.
     
    NOTE: CO-DEFENDANTS ARE NOT YET SUPPORTED BY THE SERVICE.
  """,
  )
  val prisoners: List<PrisonerDetails>,

  @Schema(description = "The court hearing type is needed if booking type is COURT, otherwise null", example = "APPEAL")
  val courtHearingType: CourtHearingType? = null,

  @Schema(
    description = "The probation meeting type is needed if booking type is PROBATION, otherwise null",
    example = "PSR",
  )
  val probationMeetingType: ProbationMeetingType? = null,

  @field:Size(max = 120, message = "The video link should not exceed {max} characters")
  @Schema(description = "The video link for the video booking. When this is provided the HMCTS number must be null.", example = "https://video.here.com")
  @Redacted
  val videoLinkUrl: String? = null,

  @field:Size(max = 8, message = "The HMCTS number should not exceed {max} characters")
  @Schema(description = "The HMCTS number for the appointment. When this is provided the video link must be null. Ignored for non-court bookings.", example = "12345678")
  @Redacted
  val hmctsNumber: String? = null,

  @field:Size(max = 8, message = "The guest PIN should not exceed {max} characters")
  @Schema(description = "The guest PIN to access the video booking. Ignored for non-court bookings.", example = "46385765")
  @Redacted
  val guestPin: String? = null,

  @Schema(
    description = """
      The additional booking details for the booking. Additional details are only applicable to probation bookings. Will
      be ignored if not a probation booking.
      """,
  )
  @field:Valid
  val additionalBookingDetails: AdditionalBookingDetails? = null,

  @field:Size(max = 400, message = "Notes for staff for the video link booking cannot not exceed {max} characters")
  @Schema(
    description = "Private free text notes for the booking.",
    example = "Legal representation details ...",
  )
  @Redacted
  val notesForStaff: String?,

  @field:Size(max = 400, message = "Notes for prisoners for the video link booking cannot not exceed {max} characters")
  @Schema(
    description = "Public free text notes for the booking. These notes are visible outside of the service, care should be taken what is entered.",
    example = "Please arrive 10 minutes early",
  )
  @Redacted
  val notesForPrisoners: String?,
) {
  @JsonIgnore
  @AssertTrue(message = "The court hearing type is mandatory for court bookings")
  private fun isInvalidCourtBooking() = (BookingType.COURT != bookingType) || (courtHearingType != null)

  @JsonIgnore
  @AssertTrue(message = "The probation probation meeting type is mandatory for probation bookings")
  private fun isInvalidProbationBooking() = (BookingType.PROBATION != bookingType) || (probationMeetingType != null)

  @JsonIgnore
  @AssertTrue(message = "The supplied video link is blank")
  private fun isInvalidUrl() = videoLinkUrl == null || videoLinkUrl.isNotBlank()

  @JsonIgnore
  @AssertTrue(message = "The video link cannot have both a video link and HMCTS number")
  private fun isInvalidCvpLinkDetails() = (BookingType.COURT != bookingType) || (videoLinkUrl == null || hmctsNumber == null)
}

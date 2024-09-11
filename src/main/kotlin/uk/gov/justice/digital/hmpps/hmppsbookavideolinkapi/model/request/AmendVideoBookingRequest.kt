package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import com.fasterxml.jackson.annotation.JsonIgnore
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

  @field:Size(max = 400, message = "Comments for the video link booking cannot not exceed {max} characters")
  @Schema(
    description = "Free text comments for the video link booking",
    example = "Waiting to hear on legal representation",
  )
  val comments: String?,

  @field:Size(max = 120, message = "The video link should not exceed {max} characters")
  @Schema(description = "The video link for the appointment.", example = "https://video.here.com")
  val videoLinkUrl: String?,
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
}

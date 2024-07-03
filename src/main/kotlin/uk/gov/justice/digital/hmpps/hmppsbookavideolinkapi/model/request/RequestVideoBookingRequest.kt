package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Past
import jakarta.validation.constraints.Size
import java.net.URI
import java.time.LocalDate

data class RequestVideoBookingRequest(

  @field:NotNull(message = "The video link booking type is mandatory")
  @Schema(description = "The booking type", example = "COURT")
  val bookingType: BookingType?,

  @field:Valid
  @field:NotEmpty(message = "At least one prisoner must be supplied for a video link booking")
  @Schema(description = "The prisoner or prisoners associated with the video link booking")
  val prisoners: List<UnknownPrisonerDetails>,

  @Schema(description = "The court code is needed if booking type is COURT, otherwise null", example = "DRBYMC")
  val courtCode: String? = null,

  @Schema(description = "The court hearing type is needed if booking type is COURT, otherwise null", example = "APPEAL")
  val courtHearingType: CourtHearingType? = null,

  @Schema(description = "The probation team code is needed if booking type is PROBATION, otherwise null", example = "BLKPPP")
  val probationTeamCode: String? = null,

  @Schema(description = "The probation meeting type is needed if booking type is PROBATION, otherwise null", example = "PSR")
  val probationMeetingType: ProbationMeetingType? = null,

  @field:Size(max = 400, message = "Comments for the video link booking cannot not exceed {max} characters")
  @Schema(description = "Free text comments for the video link booking", example = "Waiting to hear on legal representation")
  val comments: String?,

  @field:Size(max = 120, message = "The video link should not exceed {max} characters")
  @Schema(description = "The video link for the appointment. Must be a valid URL", example = "https://video.here.com")
  val videoLinkUrl: String?,
) {
  @JsonIgnore
  @AssertTrue(message = "The court code and court hearing type are mandatory for court bookings")
  private fun isInvalidCourtBooking() = (BookingType.COURT != bookingType) || (courtCode != null && courtHearingType != null)

  @JsonIgnore
  @AssertTrue(message = "The probation team code and probation meeting type are mandatory for probation bookings")
  private fun isInvalidProbationBooking() = (BookingType.PROBATION != bookingType) || (probationTeamCode != null && probationMeetingType != null)

  @JsonIgnore
  @AssertTrue(message = "The supplied video link for the appointment is not a valid URL")
  private fun isInvalidUrl() = videoLinkUrl == null || runCatching { URI(videoLinkUrl!!).toURL() }.isSuccess
}

data class UnknownPrisonerDetails(

  @field:NotEmpty(message = "Prison code is mandatory")
  @field:Size(max = 3, message = "Prison code should not exceed {max} characters")
  @Schema(description = "The prison code for the prison which the prisoner is due to arrive", example = "PVI")
  val prisonCode: String?,

  @field:NotBlank(message = "The prisoner's first name is mandatory")
  @Schema(description = "The prisoner's first name", example = "Joe")
  val firstName: String?,

  @field:NotBlank(message = "The prisoner's last name is mandatory")
  @Schema(description = "The prisoner's last name", example = "Bloggs")
  val lastName: String?,

  @field:NotNull(message = "The prisoner's date of birth is mandatory")
  @field:Past(message = "The date of birth must be in the past")
  @Schema(description = "The prisoner's date of birth", example = "1970-01-01")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  val dateOfBirth: LocalDate?,

  @field:Valid
  @field:NotEmpty(message = "At least one appointment must be supplied for the prisoner")
  @Schema(
    description =
    """
      The appointment or appointments associated with the prisoner.
  
      There should only ever be one appointment for a probation meeting.
  
      Court meetings can have up to 3 meetings, a pre, main hearing and post meeting. They must always have a main meeting.
  
      Appointment dates and times must not overlap.
    """,
  )
  val appointments: List<Appointment>,
)

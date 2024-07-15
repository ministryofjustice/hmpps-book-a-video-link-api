package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.GroupSequence
import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class CreateVideoBookingRequest(

  @field:NotNull(message = "The video link booking type is mandatory")
  @Schema(description = "The booking type", example = "COURT")
  val bookingType: BookingType?,

  @field:Valid
  @field:NotEmpty(message = "At least one prisoner must be supplied for a video link booking")
  @Schema(description = "The prisoner or prisoners associated with the video link booking")
  val prisoners: List<PrisonerDetails>,

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

enum class BookingType {
  COURT,
  PROBATION,
}

enum class CourtHearingType {
  APPEAL,
  APPLICATION,
  BACKER,
  BAIL,
  CIVIL,
  CSE,
  CTA,
  IMMIGRATION_DEPORTATION,
  FAMILY,
  TRIAL,
  FCMH,
  FTR,
  GRH,
  MDA,
  MEF,
  NEWTON,
  PLE,
  PTPH,
  PTR,
  POCA,
  REMAND,
  SECTION_28,
  SEN,
  TRIBUNAL,
  OTHER,
}

enum class ProbationMeetingType {
  PSR,
  RR,
}

data class PrisonerDetails(

  @field:NotBlank(message = "Prison code is mandatory")
  @field:Size(max = 3, message = "Prison code should not exceed {max} characters")
  @Schema(description = "The prison code for the prisoner", example = "PVI")
  val prisonCode: String?,

  @field:NotBlank(message = "Prisoner number is mandatory")
  @field:Size(max = 7, message = "Prisoner number must not exceed {max} characters")
  @Schema(description = "The prisoner number (NOMIS ID)", example = "A1234AA")
  val prisonerNumber: String?,

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

@GroupSequence(Appointment::class, DateValidationExtension::class)
data class Appointment(
  @field:NotNull(message = "The appointment type for the appointment is mandatory")
  @Schema(description = "The appointment type", example = "VLB_COURT_MAIN")
  val type: AppointmentType?,

  @field:NotBlank(message = "The location key for the appointment is mandatory")
  @field:Size(max = 160, message = "The location key should not exceed {max} characters")
  @Schema(description = "The location key for the appointment", example = "PVI-A-1-001")
  val locationKey: String?,

  @field:NotNull(message = "The date for the appointment is mandatory")
  @field:FutureOrPresent(message = "The combination of date and start time for the appointment must be in the future")
  @Schema(description = "The future date for which the appointment will start", example = "2022-12-23")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  val date: LocalDate?,

  @field:NotNull(message = "The start time for the appointment is mandatory")
  @Schema(description = "Start time for the appointment on the day", example = "10:45")
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime?,

  @field:NotNull(message = "The end time for the appointment is mandatory")
  @Schema(description = "End time for the appointment on the day", example = "11:45")
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime?,
) {
  @JsonIgnore
  @AssertTrue(message = "The end time must be after the start time for the appointment", groups = [DateValidationExtension::class])
  private fun isInvalidTime() = (startTime == null || endTime == null) || startTime.isBefore(endTime)

  @JsonIgnore
  @AssertTrue(message = "The combination of date and start time for the appointment must be in the future", groups = [DateValidationExtension::class])
  private fun isInvalidStart() = (date == null || startTime == null) || date.atTime(startTime).isAfter(LocalDateTime.now())
}

private interface DateValidationExtension

enum class AppointmentType(val isProbation: Boolean, val isCourt: Boolean) {
  // Probation types
  VLB_PROBATION(true, false),

  // Court types
  VLB_COURT_PRE(false, true),
  VLB_COURT_MAIN(false, true),
  VLB_COURT_POST(false, true),
}

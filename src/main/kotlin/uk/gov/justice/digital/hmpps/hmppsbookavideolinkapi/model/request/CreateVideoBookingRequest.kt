package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import dev.zacsweers.redacted.annotations.Redacted
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.GroupSequence
import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Schema(
  description =
  """
  Create a court or probation team meeting video link booking.
  """,
)
data class CreateVideoBookingRequest(

  @field:NotNull(message = "The video link booking type is mandatory")
  @Schema(description = "The booking type", example = "COURT")
  val bookingType: BookingType?,

  @field:Valid
  @field:NotEmpty(message = "At least one prisoner must be supplied for a video link booking")
  @Schema(
    description = """
    One or more prisoners associated with the video link booking.
    
    A probation booking should only ever have one prisoner whilst a court booking can have multiple e.g.for co-defendants.
     
    NOTE: CO-DEFENDANTS ARE NOT YET SUPPORTED BY THE SERVICE.
  """,
  )
  val prisoners: List<PrisonerDetails>,

  @Schema(description = "The court code is needed if booking type is COURT, otherwise null", example = "DRBYMC")
  val courtCode: String? = null,

  @Schema(description = "The court hearing type is needed if booking type is COURT, otherwise null", example = "APPEAL")
  val courtHearingType: CourtHearingType? = null,

  @Schema(
    description = "The probation team code is needed if booking type is PROBATION, otherwise null",
    example = "BLKPPP",
  )
  val probationTeamCode: String? = null,

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
  @Schema(description = "The HMCTS number for the video booking. When this is provided the video link must be null. Ignored for non-court bookings.", example = "12345678")
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
    example = "Some notes that will not be visible outside of the service",
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
  @AssertTrue(message = "The court code and court hearing type are mandatory for court bookings")
  private fun isInvalidCourtBooking() = (BookingType.COURT != bookingType) || (courtCode != null && courtHearingType != null)

  @JsonIgnore
  @AssertTrue(message = "The probation team code and probation meeting type are mandatory for probation bookings")
  private fun isInvalidProbationBooking() = (BookingType.PROBATION != bookingType) || (probationTeamCode != null && probationMeetingType != null)

  @JsonIgnore
  @AssertTrue(message = "The supplied video link is blank")
  private fun isInvalidUrl() = videoLinkUrl == null || videoLinkUrl.isNotBlank()

  @JsonIgnore
  @AssertTrue(message = "The video link cannot have both a video link and HMCTS number")
  private fun isInvalidCvpLinkDetails() = (BookingType.COURT != bookingType) || (videoLinkUrl == null || hmctsNumber == null)
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
  UNKNOWN,
}

enum class ProbationMeetingType(val description: String) {
  // Description added for clarity (even if not used)
  BR("Bail report"),
  HDC("Home detention curfew"),
  IOM("Integrated offender management (IOM)"),
  MALRAP("Multi-agency lifer risk assessment panel (MALRAP)"),
  OASYS("OASys"),
  OTHER("Other"),
  PR("Parole Report (PAROM)"),
  PRP("Pre-release planning"),
  PSR("Pre-sentence report (PSR)"),
  RCAT("R-CAT (recategorisation) assessments"),
  ROTL("ROTL (release on temporary licence)"),
  RR("Recall report (PRARR - parts B or C)"),
  RTSCR("Response to supervision (court report)"),
  UNKNOWN("Unknown"),
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
  @AssertTrue(
    message = "The end time must be after the start time for the appointment",
    groups = [DateValidationExtension::class],
  )
  private fun isInvalidTime() = (startTime == null || endTime == null) || startTime.isBefore(endTime)

  @JsonIgnore
  @AssertTrue(
    message = "The combination of date and start time for the appointment must be in the future",
    groups = [DateValidationExtension::class],
  )
  private fun isInvalidStart() = (date == null || startTime == null) || date.atTime(startTime).isAfter(LocalDateTime.now())
}

interface DateValidationExtension

enum class AppointmentType(val isProbation: Boolean, val isCourt: Boolean) {
  // Probation types
  VLB_PROBATION(true, false),

  // Court types
  VLB_COURT_PRE(false, true),
  VLB_COURT_MAIN(false, true),
  VLB_COURT_POST(false, true),
}

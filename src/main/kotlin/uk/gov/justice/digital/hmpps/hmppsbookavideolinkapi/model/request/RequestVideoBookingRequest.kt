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
import jakarta.validation.constraints.Past
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

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

  @field:Size(max = 120, message = "The video link should not exceed {max} characters")
  @Schema(description = "The video link for the appointment.", example = "https://video.here.com")
  @Redacted
  val videoLinkUrl: String?,

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
}

data class UnknownPrisonerDetails(

  @field:NotBlank(message = "Prison code is mandatory")
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
  val appointments: List<RequestedAppointment>,
)

@GroupSequence(RequestedAppointment::class, DateValidationExtension::class)
data class RequestedAppointment(
  @field:NotNull(message = "The appointment type for the appointment is mandatory")
  @Schema(description = "The appointment type", example = "VLB_COURT_MAIN")
  val type: AppointmentType?,

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

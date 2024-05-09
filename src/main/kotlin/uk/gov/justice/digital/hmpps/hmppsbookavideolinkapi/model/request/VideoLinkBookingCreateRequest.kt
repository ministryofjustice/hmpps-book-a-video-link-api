package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.net.URI
import java.time.LocalDate
import java.time.LocalTime

data class VideoLinkBookingCreateRequest(

  @field:NotNull(message = "The video link booking type is mandatory")
  @Schema(description = "The booking type", example = "COURT")
  val bookingType: BookingType?,

  @field:Valid
  @field:NotEmpty(message = "At least one prisoner must be supplied for a video link booking")
  @Schema(description = "The prisoner or prisoners associated with the video link booking")
  val prisoners: List<PrisonerDetails>,

  @field:Size(max = 400, message = "Comments for the video link booking cannot not exceed {max} characters")
  @Schema(description = "Free text comments for the video link booking", example = "Waiting to hear on legal representation")
  val comments: String?,
)

enum class BookingType {
  COURT,
  PROBATION,
}

data class PrisonerDetails(

  @field:NotEmpty(message = "Prison code is mandatory")
  @field:Size(max = 3, message = "Prison code should not exceed {max} characters")
  @Schema(description = "The prison code for the prisoner", example = "PVI")
  val prisonCode: String?,

  @field:NotBlank(message = "Prisoner number is mandatory")
  @field:Size(max = 7, message = "Prisoner number must not exceed {max} characters")
  @Schema(description = "The prisoner number (NOMIS ID)", example = "A1234AA")
  val prisonerNumber: String?,

  @field:Valid
  @field:NotEmpty(message = "At least one appointment must be supplied for the prisoner")
  @Schema(description = "The appointments associated with the prisoner")
  val appointments: List<Appointment>,
)

data class Appointment(
  @field:NotNull(message = "The appointment type for the appointment is mandatory")
  @Schema(description = "The appointment type", example = "TO BE DEFINED")
  val type: AppointmentType?,

  @field:NotEmpty(message = "The location key for the appointment is mandatory")
  @field:Size(max = 50, message = "The location key should not exceed {max} characters")
  @Schema(description = "The location key for the appointment", example = "PVI-A-1-001")
  val locationKey: String?,

  @field:NotNull(message = "The date for the appointment is mandatory")
  @field:Future(message = "The date for the appointment must be in the future")
  @Schema(description = "The future date for which the appointment will start", example = "2022-12-23")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
  val date: LocalDate?,

  @field:NotNull(message = "The start time for the appointment is mandatory")
  @Schema(description = "Start time for the appointment on the day", example = "10:45")
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime?,

  @field:NotNull(message = "The end time for the appointment is mandatory")
  @Schema(description = "End time for the appointment on the day", example = "11:45")
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime?,

  @field:Size(max = 120, message = "The video link should not exceed {max} characters")
  @Schema(description = "The video link for the appointment. Must be a valid URL", example = "https://video.here.com")
  val videoLinkUrl: String?,
) {
  @AssertTrue(message = "The supplied video link for the appointment is not a valid URL")
  private fun isValidUrl() = videoLinkUrl == null || runCatching { URI(videoLinkUrl!!).toURL() }.isSuccess

  @AssertTrue(message = "The end time must be after the start time for the appointment")
  private fun isValidTime() = (startTime == null || endTime == null) || startTime.isBefore(endTime)
}

enum class AppointmentType {
  TBD,
}

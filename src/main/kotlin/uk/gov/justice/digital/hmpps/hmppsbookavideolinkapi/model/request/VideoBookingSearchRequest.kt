package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.BookingStatus
import java.time.LocalDate
import java.time.LocalTime

@Schema(
  description =
  """
  Search attributes to search for a unique video link booking.
  
  The prisoner number, location key, date, start and end time combined should provide sufficient information to find a
  unique video link booking.
  """,
)
data class VideoBookingSearchRequest(
  @field:NotBlank(message = "The prisoner number is mandatory")
  @Schema(description = "The prisoner number (NOMIS ID)", example = "A1234AA")
  val prisonerNumber: String?,

  @field:NotBlank(message = "The location key is mandatory")
  @Schema(description = "The location key for the appointment", example = "PVI-A-1-001")
  val locationKey: String?,

  @field:NotNull(message = "The date is mandatory")
  @Schema(description = "The date for which the appointment starts", example = "2022-12-23")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  val date: LocalDate?,

  @field:NotNull(message = "The start time is mandatory")
  @Schema(description = "Start time for the appointment on the day", example = "10:45")
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime?,

  @field:NotNull(message = "The end time is mandatory")
  @Schema(description = "End time for the appointment on the day", example = "11:45")
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime?,

  @Schema(description = "The status of the booking to match, defaults to ACTIVE", example = "ACTIVE")
  val statusCode: BookingStatus? = BookingStatus.ACTIVE,
)

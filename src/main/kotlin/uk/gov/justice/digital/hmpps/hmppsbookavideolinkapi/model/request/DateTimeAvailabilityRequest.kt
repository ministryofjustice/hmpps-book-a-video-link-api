package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalTime

data class DateTimeAvailabilityRequest(
  @field:NotBlank(message = "The prison code is mandatory")
  @field:Size(max = 3, message = "Prison code should not exceed {max} characters")
  @Schema(description = "The prison code for the prisoner", example = "PVI", required = true)
  val prisonCode: String?,

  @field:NotNull(message = "The booking type is mandatory")
  @Schema(description = "The booking type", example = "PROBATION", required = true)
  val bookingType: BookingType?,

  @field:Size(max = 40, message = "Court code should not exceed {max} characters")
  @Schema(description = "The court code is needed if booking type is COURT, otherwise null", example = "DRBYMC")
  val courtCode: String? = null,

  @field:Size(max = 40, message = "Probation team code should not exceed {max} characters")
  @Schema(
    description = "The probation team code is needed if booking type is PROBATION, otherwise null",
    example = "BLKPPP",
  )
  val probationTeamCode: String? = null,

  @field:NotNull(message = "The date is mandatory")
  @field:FutureOrPresent(message = "The date must be future or present")
  @Schema(description = "The present or future date when the room is needed", example = "2050-01-01", required = true)
  val date: LocalDate?,

  @field:NotNull(message = "The start time is mandatory")
  @Schema(description = "Start time for on the day", example = "10:45")
  @JsonFormat(pattern = "HH:mm")
  val startTime: LocalTime?,

  @field:NotNull(message = "The end time is mandatory")
  @Schema(description = "End time for on the day", example = "11:45")
  @JsonFormat(pattern = "HH:mm")
  val endTime: LocalTime?,

  @Schema(
    description = "Exclude the video link booking with this ID from the availability check. Useful when checking availability during the amending of a booking.",
  )
  val vlbIdToExclude: Long? = null,
) {
  @JsonIgnore
  @AssertTrue(message = "The court code is mandatory for court bookings")
  private fun isCourtBooking() = (BookingType.COURT != bookingType) || (courtCode != null)

  @JsonIgnore
  @AssertTrue(message = "The probation team code is mandatory for probation bookings")
  private fun isProbationBooking() = (BookingType.PROBATION != bookingType) || (probationTeamCode != null)
}

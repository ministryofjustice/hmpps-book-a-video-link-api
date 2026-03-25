package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotEmpty
import java.time.LocalDate

@Schema(description = "Request to find probation bookings for one or more teams")
data class FindProbationBookingsRequest(
  @Schema(description = "A date in ISO format (YYYY-MM-DD). Defaults to today if not supplied.")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  val fromDate: LocalDate = LocalDate.now(),

  @Schema(description = "A date in ISO format (YYYY-MM-DD) on or after the from date and a maximum of thirty days after the from date.")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  val toDate: LocalDate? = null,

  @field:NotEmpty(message = "At least one probation team code is required")
  @Schema(description = "A list of probation team codes to find bookings for", example = "[CODE1, CODE2, CODE3, CODE4]")
  val probationTeamCodes: List<String>?,
) {
  @JsonIgnore
  @AssertTrue(message = "The to date must be on or after the from date")
  private fun isInvalidDates() = toDate == null || !toDate.isBefore(fromDate)

  @JsonIgnore
  @AssertTrue(message = "The to date must be a maximum of thirty days after the from date")
  private fun isInvalidDateRange() = toDate == null || !toDate.minusDays(30).isAfter(fromDate)
}

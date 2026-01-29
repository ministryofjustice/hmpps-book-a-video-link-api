package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import java.time.LocalDate

@Schema(description = "Request to find probation bookings for one or more teams")
data class FindProbationBookingsRequest(
  @Schema(description = "A date in ISO format (YYYY-MM-DD). Defaults to today if not supplied.")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  val date: LocalDate = LocalDate.now(),

  @field:NotEmpty(message = "At least one probation team code is required")
  @Schema(description = "A list of probation team codes to find bookings for", example = "[CODE1, CODE2, CODE3, CODE4]")
  val probationTeamCodes: List<String>?,
)

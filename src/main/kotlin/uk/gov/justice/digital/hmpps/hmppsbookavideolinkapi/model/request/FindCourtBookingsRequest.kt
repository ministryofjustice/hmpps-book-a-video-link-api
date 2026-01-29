package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import java.time.LocalDate

@Schema(description = "Request to find court bookings for one or more courts")
data class FindCourtBookingsRequest(
  @Schema(description = "A date in ISO format (YYYY-MM-DD). Defaults to today if not supplied.")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  val date: LocalDate = LocalDate.now(),

  @field:NotEmpty(message = "At least one court code is required")
  @Schema(description = "A list of court codes to find bookings for", example = "[CODE1, CODE2, CODE3, CODE4]")
  val courtCodes: List<String>?,
)

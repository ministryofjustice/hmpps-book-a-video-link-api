package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.SubjectAccessRequestDto
import java.time.LocalDate

data class SubjectAccessResponseData(
  @Schema(description = "The prisoner number (Nomis ID)", example = "A1234AA")
  val prisonerNumber: String,

  @Schema(description = "The from date for the request", example = "2022-01-01")
  val fromDate: LocalDate,

  @Schema(description = "The to date for the request", example = "2024-01-01")
  val toDate: LocalDate,

  @Schema(description = "All of the allocations for the prisoner for the period")
  val videoLinkBookings: List<SubjectAccessRequestDto>,
)

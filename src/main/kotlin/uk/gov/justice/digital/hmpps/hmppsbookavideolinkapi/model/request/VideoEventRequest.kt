package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

data class VideoEventRequest(

  @Schema(description = "The start date for events to retrieve", example = "2022-12-23", required = true)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  val startDate: LocalDate,

  @Schema(description = "The end date for events to retrieve", example = "2022-12-23", required = true)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd")
  val endDate: LocalDate,
)

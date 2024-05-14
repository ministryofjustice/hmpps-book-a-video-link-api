package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes the details of a court")
data class Court(

  @Schema(description = "An internally-generated unique identifier for this court.", example = "12345")
  val courtId: Long,

  @Schema(description = "A short code for this court.", example = "AVONCC")
  val code: String,

  @Schema(description = "A fuller description for this court", example = "Avon Crown Court")
  val description: String,

  @Schema(description = "A boolean value to show whether enabled for video link bookings.", example = "true")
  val enabled: Boolean,

  @Schema(description = "Notes relating to this court for opening hours, postal address, main contact.", example = "Free form notes")
  val notes: String?,
)

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

data class AdditionalBookingDetails(
  @Schema(description = "The name of the contact")
  val contactName: String,

  @Schema(description = "The email address for the contact, must be a valid email address")
  val contactEmail: String,

  @Schema(description = "The contact phone number for the contact, must be a valid phone number")
  val contactNumber: String?,
)

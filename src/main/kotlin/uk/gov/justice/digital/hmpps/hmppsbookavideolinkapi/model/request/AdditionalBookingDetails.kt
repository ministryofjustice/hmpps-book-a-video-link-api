package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(
  description =
  """
  Additional information for the booking. Must provide at least one form of contact email and/or phone number.
  """,
)
data class AdditionalBookingDetails(
  @field:NotBlank(message = "Contact name is mandatory")
  @field:Size(max = 100, message = "Contact name should not exceed {max} characters")
  val contactName: String?,

  @field:NotBlank(message = "Contact email is mandatory")
  @field:Size(max = 100, message = "Contact email should not exceed {max} characters")
  @Schema(description = "The email address for the contact, must be a valid email address")
  val contactEmail: String?,

  @field:Size(max = 30, message = "Contact phone number should not exceed {max} characters")
  @Schema(description = "The contact phone number for the contact, must be a valid phone number")
  val contactNumber: String?,
)

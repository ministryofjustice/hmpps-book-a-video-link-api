package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(
  description =
  """
  Additional information for the booking. Must provide at least one form of contact email and/or phone number.
  """,
)
data class AdditionalBookingDetails(
  @field:NotBlank(message = "Name is mandatory")
  @field:Size(max = 100, message = "Contact name should not exceed {max} characters")
  val contactName: String?,

  @field:Size(max = 100, message = "Contact email should not exceed {max} characters")
  @Schema(description = "The email address for the contact, must be a valid email address")
  val contactEmail: String?,

  @field:Size(max = 30, message = "Contact phone number should not exceed {max} characters")
  @Schema(description = "The contact phone number for the contact, must be a valid phone number")
  val contactNumber: String?,

  @field:Size(max = 100, message = "Extra information should not exceed {max} characters")
  val extraInformation: String?,
) {
  @JsonIgnore
  @AssertTrue(message = "Please provide an email address, contact number or both")
  private fun isContactDetails() = contactEmail != null || contactNumber != null
}

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes the details of a booking contact")
data class BookingContact(
  @Schema(description = "Describes the internal id of the video booking", example = "123")
  val videoBookingId: Long,

  @Schema(description = "Describes the contact type (OWNER, PRISON, PROBATION, COURT, THIRD_PARTY)", example = "PRISON")
  val contactType: String,

  @Schema(description = "Describes the contact name (optional)", example = "Mr. Person-contact")
  val name: String?,

  @Schema(description = "Describes the position or role of the contact person (optional)", example = "BVLS Administator")
  val position: String?,

  @Schema(description = "Describes the email address of this contact (optional)", example = "example@example.com")
  val email: String?,

  @Schema(description = "Describes the telephone number of this contact (optional)", example = "00902 0909779")
  val telephone: String?,
)

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes the details of a booking contact")
data class BookingContact(
  @Schema(description = "Describes the internal id of the video booking", example = "123")
  val videoBookingId: Long,

  @Schema(description = "Describes the contact type", example = "PRISON")
  val contactType: ContactType,

  @Schema(description = "Describes the contact name (optional)", example = "Mr. Person-contact")
  val name: String?,

  @Schema(
    description = "Describes the position or role of the contact person (optional)",
    example = "BVLS Administrator",
  )
  val position: String? = null,

  @Schema(description = "Describes the email address of this contact (optional)", example = "example@example.com")
  val email: String?,

  @Schema(description = "Describes the telephone number of this contact (optional)", example = "00902 0909779")
  val telephone: String? = null,

  @Schema(
    description = """
    Describes the whether the contact is a primary contact or not, true if yes otherwise false.
    
    There will only ever be one primary contact for each contact type e.g. a court will only have one primary contact.
  """,
    example = "true",
  )
  val primaryContact: Boolean,
)

enum class ContactType {
  OWNER,
  COURT,
  PROBATION,
  PRISON,
  THIRD_PARTY,
}

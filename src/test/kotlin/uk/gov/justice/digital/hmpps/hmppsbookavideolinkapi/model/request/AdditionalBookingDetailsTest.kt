package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import org.junit.jupiter.api.Test

class AdditionalBookingDetailsTest : ValidatorBase<AdditionalBookingDetails>() {

  private val details = AdditionalBookingDetails(
    contactName = "Fred",
    contactEmail = "valid@email.address",
    contactNumber = "0114 2345678",
  )

  @Test
  fun `should be no errors for valid details`() {
    assertNoErrors(details)
    assertNoErrors(details.copy(contactNumber = null))
  }

  @Test
  fun `should fail when no email address`() {
    details.copy(contactEmail = null) failsWithSingle ModelError("contactEmail", "Contact email is mandatory")
  }
}

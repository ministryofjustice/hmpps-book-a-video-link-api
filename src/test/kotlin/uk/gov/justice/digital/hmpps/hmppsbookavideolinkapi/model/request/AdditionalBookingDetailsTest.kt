package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import org.junit.jupiter.api.Test

class AdditionalBookingDetailsTest : ValidatorBase<AdditionalBookingDetails>() {

  private val details = AdditionalBookingDetails(
    contactName = "Fred",
    contactEmail = "valid@email.address",
    contactNumber = "0114 2345678",
    extraInformation = "extra information",
  )

  @Test
  fun `should be no errors for valid details`() {
    assertNoErrors(details)
    assertNoErrors(details.copy(contactEmail = null))
    assertNoErrors(details.copy(contactNumber = null))
    assertNoErrors(details.copy(extraInformation = null))
  }

  @Test
  fun `should fail when no contact information`() {
    details.copy(contactEmail = null, contactNumber = null) failsWithSingle ModelError("contactDetails", "Please provide an email address, contact number or both")
  }

  @Test
  fun `should fail when extra details blank`() {
    details.copy(contactEmail = null, contactNumber = null) failsWithSingle ModelError("contactDetails", "Please provide an email address, contact number or both")
  }
}

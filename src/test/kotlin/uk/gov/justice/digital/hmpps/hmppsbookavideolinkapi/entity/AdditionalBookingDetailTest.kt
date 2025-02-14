package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo

class AdditionalBookingDetailTest {

  private val booking = courtBooking()

  @Test
  fun `should reject blank or empty contact name`() {
    assertThrows<IllegalArgumentException> {
      AdditionalBookingDetail.newDetails(booking, contactName = "", contactEmail = "email@somehwere.com", contactPhoneNumber = "124")
    }.message isEqualTo "Contact name cannot be blank"

    assertThrows<IllegalArgumentException> {
      AdditionalBookingDetail.newDetails(booking, contactName = "   ", contactEmail = "email@somehwere.com", contactPhoneNumber = "124")
    }.message isEqualTo "Contact name cannot be blank"
  }

  @Test
  fun `should reject invalid contact email`() {
    assertThrows<IllegalArgumentException> {
      AdditionalBookingDetail.newDetails(booking, contactName = "contact", contactEmail = " ", contactPhoneNumber = "124")
    }.message isEqualTo "Contact email is not a valid email address"

    assertThrows<IllegalArgumentException> {
      AdditionalBookingDetail.newDetails(booking, contactName = "contact", contactEmail = "invalid_email", contactPhoneNumber = "124")
    }.message isEqualTo "Contact email is not a valid email address"

    assertThrows<IllegalArgumentException> {
      AdditionalBookingDetail.newDetails(booking, contactName = "contact", contactEmail = "invalid@@email.com", contactPhoneNumber = "124")
    }.message isEqualTo "Contact email is not a valid email address"
  }

  @Test
  fun `should reject invalid phone number`() {
    assertThrows<IllegalArgumentException> {
      AdditionalBookingDetail.newDetails(booking, contactName = "contact", contactEmail = "valid@email.address", contactPhoneNumber = "124")
    }.message isEqualTo "Contact number is not a valid UK phone number"
  }

  @Test
  fun `should be valid additional information`() {
    AdditionalBookingDetail.newDetails(booking, contactName = "contact", contactEmail = "valid@email.address", contactPhoneNumber = null)

    with(AdditionalBookingDetail.newDetails(booking, contactName = "contact", contactEmail = "valid@email.address", contactPhoneNumber = "01234 567890")) {
      videoBooking isEqualTo booking
      contactName isEqualTo "contact"
      contactEmail isEqualTo "valid@email.address"
      contactNumber isEqualTo "01234 567890"
    }
  }
}

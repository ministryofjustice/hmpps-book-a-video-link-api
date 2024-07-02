package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsEntriesExactlyInAnyOrder
import java.time.LocalDate

class NewCourtBookingOwnerEmailTest {

  @Test
  fun `should personalise court new booking email`() {
    val email = NewCourtBookingOwnerEmail(
      address = "address 1",
      prisonerFirstName = "builder",
      prisonerLastName = "bob",
      prisonerNumber = "123456",
      date = LocalDate.of(2200,
        12, 12),
      userName = "username",
      comments = "comments for bob",
      preAppointmentInfo = "bobs pre-appointment info",
      mainAppointmentInfo = "bobs main appointment info",
      postAppointmentInfo = "bob post appointment info",
      court = "the court",
      prison = "the prison",
    )

    email.personalisation() containsEntriesExactlyInAnyOrder mapOf(
      "date" to "12 Dec 2200",
      "prisonerName" to "builder bob",
      "offenderNo" to "123456",
      "comments" to "comments for bob",
      "userName" to "username",
      "court" to "the court",
      "prison" to "the prison",
      "preAppointmentInfo" to "bobs pre-appointment info",
      "mainAppointmentInfo" to "bobs main appointment info",
      "postAppointmentInfo" to "bob post appointment info",
    )
  }
}

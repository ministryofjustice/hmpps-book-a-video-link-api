package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.yesterday

class FindCourtBookingsRequestTest : ValidatorBase<FindCourtBookingsRequest>() {
  @Test
  fun `should be no errors for valid requests`() {
    assertNoErrors(FindCourtBookingsRequest(fromDate = today(), toDate = null, courtCodes = listOf("CODE")))
    assertNoErrors(FindCourtBookingsRequest(fromDate = today(), toDate = tomorrow(), listOf("CODE")))
  }

  @Test
  fun `should be errors for invalid requests`() {
    FindCourtBookingsRequest(fromDate = today(), toDate = null, courtCodes = null) failsWithSingle ModelError("courtCodes", "At least one court code is required")
    FindCourtBookingsRequest(fromDate = today(), toDate = yesterday(), courtCodes = listOf("CODE")) failsWithSingle ModelError("invalidDates", "The to date must be on or after the from date")
    FindCourtBookingsRequest(fromDate = today(), toDate = today().plusDays(31), courtCodes = listOf("CODE")) failsWithSingle ModelError("invalidDateRange", "The to date must be a maximum of thirty days after the from date")
  }
}

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.yesterday

class FindProbationBookingsRequestTest : ValidatorBase<FindProbationBookingsRequest>() {
  @Test
  fun `should be no errors for valid requests`() {
    assertNoErrors(FindProbationBookingsRequest(fromDate = today(), toDate = null, probationTeamCodes = listOf("CODE")))
    assertNoErrors(FindProbationBookingsRequest(fromDate = today(), toDate = tomorrow(), probationTeamCodes = listOf("CODE")))
  }

  @Test
  fun `should be errors for invalid requests`() {
    FindProbationBookingsRequest(fromDate = today(), toDate = null, probationTeamCodes = null) failsWithSingle ModelError("probationTeamCodes", "At least one probation team code is required")
    FindProbationBookingsRequest(fromDate = today(), toDate = yesterday(), probationTeamCodes = listOf("CODE")) failsWithSingle ModelError("invalidDates", "The to date must be on or after the from date")
    FindProbationBookingsRequest(fromDate = today(), toDate = today().plusDays(31), probationTeamCodes = listOf("CODE")) failsWithSingle ModelError("invalidDateRange", "The to date must be a maximum of thirty days after the from date")
  }
}

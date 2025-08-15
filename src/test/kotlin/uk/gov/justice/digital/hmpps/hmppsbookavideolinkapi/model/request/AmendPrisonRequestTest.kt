package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import org.junit.jupiter.api.Test

class AmendPrisonRequestTest : ValidatorBase<AmendPrisonRequest>() {

  private val amendRequest = AmendPrisonRequest(30)

  @Test
  fun `should be no errors for valid request`() {
    assertNoErrors(amendRequest)
    assertNoErrors(amendRequest.copy(pickUpTime = null))

    IntRange(1, 60).forEach {
      assertNoErrors(amendRequest.copy(pickUpTime = it))
    }
  }

  @Test
  fun `should fail when exceeds the max allowed pick-up time`() {
    amendRequest.copy(pickUpTime = 61) failsWithSingle ModelError("invalidPickUpTime", "The pick-up time must be between 1 to 60 minutes")
  }

  @Test
  fun `should fail when precedes the min allowed pick-up time`() {
    amendRequest.copy(pickUpTime = 0) failsWithSingle ModelError("invalidPickUpTime", "The pick-up time must be between 1 to 60 minutes")
  }
}

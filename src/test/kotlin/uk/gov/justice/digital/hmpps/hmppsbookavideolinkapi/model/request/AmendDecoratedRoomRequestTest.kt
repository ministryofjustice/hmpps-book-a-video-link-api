package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.yesterday
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationUsage

class AmendDecoratedRoomRequestTest : ValidatorBase<AmendDecoratedRoomRequest>() {

  private val amendRequest = AmendDecoratedRoomRequest(
    locationUsage = LocationUsage.COURT,
    locationStatus = LocationStatus.ACTIVE,
    allowedParties = setOf("COURT"),
    prisonVideoUrl = "v".repeat(300),
    comments = "amended comments",
  )

  private val amendBlockedRequest = AmendDecoratedRoomRequest(
    locationUsage = LocationUsage.SHARED,
    locationStatus = LocationStatus.TEMPORARILY_BLOCKED,
    blockedFrom = yesterday(),
    blockedTo = today(),
  )

  @Test
  fun `should be no errors for valid request`() {
    assertNoErrors(amendRequest)
  }

  @Test
  fun `should fail when prison video url exceeds max allowed characters`() {
    amendRequest.copy(prisonVideoUrl = "v".repeat(301)) failsWithSingle ModelError("prisonVideoUrl", "Prison video URL should not exceed 300 characters")
  }

  @Test
  fun `should fail when blocked to is in the past`() {
    amendBlockedRequest.copy(blockedTo = yesterday()) failsWithSingle ModelError("blockedTo", "The blocked to date must be in the future or present")
  }

  @Test
  fun `should fail when blocked to is before the blocked from`() {
    amendBlockedRequest.copy(blockedFrom = tomorrow(), blockedTo = today()) failsWithSingle ModelError("invalidBlocked", "The blocked to must be on or after the blocked from date")
  }
}

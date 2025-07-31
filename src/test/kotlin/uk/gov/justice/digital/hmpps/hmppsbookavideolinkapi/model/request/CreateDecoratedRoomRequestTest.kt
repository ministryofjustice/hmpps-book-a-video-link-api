package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationUsage

class CreateDecoratedRoomRequestTest : ValidatorBase<CreateDecoratedRoomRequest>() {

  private val createRequest = CreateDecoratedRoomRequest(
    locationUsage = LocationUsage.COURT,
    locationStatus = LocationStatus.ACTIVE,
    allowedParties = setOf("COURT"),
    prisonVideoUrl = "v".repeat(300),
    comments = "amended comments",
  )

  @Test
  fun `should be no errors for valid request`() {
    assertNoErrors(createRequest)
  }

  @Test
  fun `should fail when prison video url exceeds max allowed characters`() {
    createRequest.copy(prisonVideoUrl = "v".repeat(301)) failsWithSingle ModelError("prisonVideoUrl", "Prison video URL should not exceed 300 characters")
  }
}

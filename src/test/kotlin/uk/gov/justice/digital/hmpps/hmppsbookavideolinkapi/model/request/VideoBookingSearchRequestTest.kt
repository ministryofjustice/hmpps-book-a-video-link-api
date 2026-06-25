package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import java.time.LocalTime

class VideoBookingSearchRequestTest : ValidatorBase<VideoBookingSearchRequest>() {
  private val request = VideoBookingSearchRequest(
    prisonerNumber = "123456",
    locationKey = wandsworthLocation.key,
    date = tomorrow(),
    startTime = LocalTime.of(12, 0),
    endTime = LocalTime.of(13, 0),
    dpsLocationId = wandsworthLocation.id,
  )

  @Test
  fun `should be no errors for valid requests`() {
    assertNoErrors(request)
    assertNoErrors(request.copy(locationKey = null))
    assertNoErrors(request.copy(dpsLocationId = null))
  }

  @Test
  fun `should fail when location id and key are missing`() {
    request.copy(dpsLocationId = null, locationKey = null) failsWithSingle ModelError("invalidLocation", "You must provide either a location id or a location key for search")
  }
}

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.LocationsInsidePrisonApiMockServer

class LocationsInsidePrisonClientTest {

  private val locationKey = "MDI-A-1-001"
  private val server = LocationsInsidePrisonApiMockServer().also { it.start() }
  private val client = LocationsInsidePrisonClient(WebClient.create("http://localhost:${server.port()}"))

  @Test
  fun `should get matching location`() {
    server.stubPostLocationByKeys(listOf(locationKey))

    client.getLocationsByKeys(listOf(locationKey)).single().key isEqualTo locationKey
  }

  @AfterEach
  fun after() {
    server.stop()
  }
}

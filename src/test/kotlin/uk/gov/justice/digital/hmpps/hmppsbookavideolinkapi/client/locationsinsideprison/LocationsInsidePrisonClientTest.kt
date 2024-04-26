package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.health.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.LocationsInsidePrisonApiMockServer
import java.util.UUID

class LocationsInsidePrisonClientTest {

  private val locationId = UUID.randomUUID()
  private val server = LocationsInsidePrisonApiMockServer().also { it.start() }
  private val client = LocationsInsidePrisonClient(WebClient.create("http://localhost:${server.port()}"))

  @Test
  fun `should get matching location`() {
    server.stubGetLocation(locationId)

    client.getLocation(locationId)?.id isEqualTo locationId
  }

  @AfterEach
  fun after() {
    server.stop()
  }
}

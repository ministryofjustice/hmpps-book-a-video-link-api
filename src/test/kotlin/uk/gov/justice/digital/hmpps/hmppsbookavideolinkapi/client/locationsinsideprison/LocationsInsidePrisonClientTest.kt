package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvilleLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.LocationsInsidePrisonApiMockServer

class LocationsInsidePrisonClientTest {

  private val locationKey = "WWI-A-1-001"
  private val server = LocationsInsidePrisonApiMockServer().also { it.start() }
  private val client = LocationsInsidePrisonClient(WebClient.create("http://localhost:${server.port()}"))

  @Test
  fun `should get matching location`() {
    server.stubPostLocationByKeys(setOf(locationKey))

    client.getLocationsByKeys(setOf(locationKey)).single().key isEqualTo locationKey
  }

  @Test
  fun `should only return leaf level video link locations`() {
    server.stubVideoLinkLocationsAtPrison(setOf(wandsworthLocation.key), leafLevel = true)
    client.getVideoLinkLocationsAtPrison(WANDSWORTH).single().key isEqualTo wandsworthLocation.key

    server.stubVideoLinkLocationsAtPrison(setOf(pentonvilleLocation.key), leafLevel = false)
    client.getVideoLinkLocationsAtPrison(PENTONVILLE) hasSize 0
  }

  @Test
  fun `should only return leaf level non-residential locations`() {
    server.stubNonResidentialAppointmentLocationsAtPrison(setOf(wandsworthLocation.key), leafLevel = true)
    client.getNonResidentialAppointmentLocationsAtPrison(WANDSWORTH).single().key isEqualTo wandsworthLocation.key

    server.stubNonResidentialAppointmentLocationsAtPrison(setOf(pentonvilleLocation.key), leafLevel = false)
    client.getNonResidentialAppointmentLocationsAtPrison(PENTONVILLE) hasSize 0
  }

  @AfterEach
  fun after() {
    server.stop()
  }
}

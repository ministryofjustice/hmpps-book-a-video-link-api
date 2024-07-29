package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WERRINGTON
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.werringtonLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.LocationsInsidePrisonApiMockServer

class LocationsInsidePrisonClientTest {

  private val locationKey = "MDI-A-1-001"
  private val server = LocationsInsidePrisonApiMockServer().also { it.start() }
  private val client = LocationsInsidePrisonClient(WebClient.create("http://localhost:${server.port()}"))

  @Test
  fun `should get matching location`() {
    server.stubPostLocationByKeys(setOf(locationKey))

    client.getLocationsByKeys(setOf(locationKey)).single().key isEqualTo locationKey
  }

  @Test
  fun `should only return leaf level video link locations`() {
    server.stubVideoLinkLocationsAtPrison(setOf(moorlandLocation.key), leafLevel = true)
    client.getVideoLinkLocationsAtPrison(MOORLAND).single().key isEqualTo moorlandLocation.key

    server.stubVideoLinkLocationsAtPrison(setOf(werringtonLocation.key), leafLevel = false)
    client.getVideoLinkLocationsAtPrison(WERRINGTON) hasSize 0
  }

  @Test
  fun `should only return leaf level non-residential locations`() {
    server.stubNonResidentialAppointmentLocationsAtPrison(setOf(moorlandLocation.key), leafLevel = true)
    client.getNonResidentialAppointmentLocationsAtPrison(MOORLAND).single().key isEqualTo moorlandLocation.key

    server.stubNonResidentialAppointmentLocationsAtPrison(setOf(werringtonLocation.key), leafLevel = false)
    client.getNonResidentialAppointmentLocationsAtPrison(WERRINGTON) hasSize 0
  }

  @AfterEach
  fun after() {
    server.stop()
  }
}

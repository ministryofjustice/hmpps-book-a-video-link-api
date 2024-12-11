package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvilleLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.LocationsInsidePrisonApiMockServer

class LocationsInsidePrisonClientTest {

  private val locationKey = "WWI-A-1-001"
  private val server = LocationsInsidePrisonApiMockServer().also { it.start() }
  private val client = LocationsInsidePrisonClient(WebClient.create("http://localhost:${server.port()}"))

  @Test
  fun `should get matching location by id`() {
    val location = location(WANDSWORTH, locationKeySuffix = "A-1-001")
    server.stubGetLocationById(location)

    client.getLocationById(location.id)?.key isEqualTo locationKey
  }

  @Test
  fun `should get matching location by key`() {
    server.stubGetLocationByKey(location(WANDSWORTH, locationKeySuffix = "A-1-001"))

    client.getLocationByKey(locationKey)!!.key isEqualTo locationKey
  }

  @Test
  fun `should get matching location by list of keys`() {
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
  fun `should return all non-residential locations`() {
    server.stubNonResidentialAppointmentLocationsAtPrison(WANDSWORTH, wandsworthLocation.copy(leafLevel = false))
    client.getNonResidentialAppointmentLocationsAtPrison(WANDSWORTH).single().key isEqualTo wandsworthLocation.key

    server.stubNonResidentialAppointmentLocationsAtPrison(PENTONVILLE, pentonvilleLocation.copy(leafLevel = false))
    client.getNonResidentialAppointmentLocationsAtPrison(PENTONVILLE).single().key isEqualTo pentonvilleLocation.key
  }

  @AfterEach
  fun after() {
    server.stop()
  }
}

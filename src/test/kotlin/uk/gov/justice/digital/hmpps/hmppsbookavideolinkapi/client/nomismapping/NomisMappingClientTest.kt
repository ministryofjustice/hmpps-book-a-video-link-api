package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.nomismapping

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation2
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation3
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.NomisMappingApiMockServer
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.nomisDpsLocationMappings

class NomisMappingClientTest {
  private val server = NomisMappingApiMockServer().also { it.start() }
  private val client = NomisMappingClient(WebClient.create("http://localhost:${server.port()}"))

  @Test
  fun `should get matching NOMIS DPS location mappings by DPS location IDs`() {
    server.stubPostNomisLocationMappingsBy(listOf(wandsworthLocation, wandsworthLocation2, wandsworthLocation3))

    client.getNomisLocationMappingsBy(
      DpsLocationsIds(listOf(wandsworthLocation.id, wandsworthLocation2.id, wandsworthLocation3.id)),
    ) containsExactlyInAnyOrder listOf(
      nomisDpsLocationMappings[wandsworthLocation],
      nomisDpsLocationMappings[wandsworthLocation2],
      nomisDpsLocationMappings[wandsworthLocation3],
    )
  }

  @AfterEach
  fun after() {
    server.stop()
  }
}

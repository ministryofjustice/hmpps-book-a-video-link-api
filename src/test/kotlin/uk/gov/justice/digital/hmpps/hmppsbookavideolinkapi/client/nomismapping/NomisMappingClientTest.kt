package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.nomismapping

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation2
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation3
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.NomisMappingApiMockServer

class NomisMappingClientTest {
  private val server = NomisMappingApiMockServer().also { it.start() }
  private val client = NomisMappingClient(WebClient.create("http://localhost:${server.port()}"))

  @Test
  fun `should get matching NOMIS DPS location mappings by DPS location IDs`() {
    server.stubPostNomisLocationMappingsBy(
      listOf(
        NomisDpsLocationMapping(wandsworthLocation.id, 1),
        NomisDpsLocationMapping(wandsworthLocation2.id, 2),
        NomisDpsLocationMapping(wandsworthLocation3.id, 3),
      ),
    )

    client.getNomisLocationMappingsBy(
      DpsLocationsIds(listOf(wandsworthLocation.id, wandsworthLocation2.id, wandsworthLocation3.id)),
    ) containsExactlyInAnyOrder listOf(
      NomisDpsLocationMapping(wandsworthLocation.id, 1),
      NomisDpsLocationMapping(wandsworthLocation2.id, 2),
      NomisDpsLocationMapping(wandsworthLocation3.id, 3),
    )
  }

  @AfterEach
  fun after() {
    server.stop()
  }
}

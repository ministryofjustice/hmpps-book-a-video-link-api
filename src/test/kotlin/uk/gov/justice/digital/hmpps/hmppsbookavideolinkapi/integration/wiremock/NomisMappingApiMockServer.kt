package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.nomismapping.DpsLocationsIds
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.nomismapping.NomisDpsLocationMapping
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.allLocations

val nomisDpsLocationMappings = allLocations.associateBy(
  { it },
  { NomisDpsLocationMapping(it.id, allLocations.indexOf(it).toLong()) },
)

class NomisMappingApiMockServer : MockServer(8096) {
  fun stubPostNomisLocationMappingsBy(locations: List<Location>) {
    stubFor(
      post("/api/locations/dps")
        .withRequestBody(
          equalToJson(mapper.writeValueAsString(DpsLocationsIds(locations.map(Location::id)).locationIds)),
        ).willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(locations.map { nomisDpsLocationMappings[it] }))
            .withStatus(200),
        ),
    )
  }

  fun stubGetNomisLocationMappingBy(location: Location, nomisLocationId: Long = 1) {
    stubFor(
      get("/api/locations/dps/${location.id}").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(mapper.writeValueAsString(NomisDpsLocationMapping(location.id, nomisLocationId)))
          .withStatus(200),
      ),
    )
  }
}

class NomisMappingApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val server = NomisMappingApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    server.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    server.resetAll()
  }

  override fun afterAll(context: ExtensionContext) {
    server.stop()
  }
}

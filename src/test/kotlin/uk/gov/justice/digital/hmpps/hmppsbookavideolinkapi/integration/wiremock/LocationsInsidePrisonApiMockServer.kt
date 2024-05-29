package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import java.util.UUID

class LocationsInsidePrisonApiMockServer : MockServer(8091) {

  fun stubPostLocationByKeys(keys: Set<String>, prisonId: String = "MDI") {
    stubFor(
      post("/locations/keys")
        .withRequestBody(WireMock.equalToJson(mapper.writeValueAsString(keys)))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                keys.map { key ->
                  Location(
                    id = UUID.randomUUID(),
                    prisonId = prisonId,
                    code = "001",
                    pathHierarchy = "A-1-001",
                    locationType = Location.LocationType.VIDEO_LINK,
                    permanentlyInactive = false,
                    active = true,
                    deactivatedByParent = false,
                    topLevelId = UUID.randomUUID(),
                    key = key,
                    isResidential = true,
                  )
                },
              ),
            )
            .withStatus(200),
        ),
    )
  }
}

class LocationsInsidePrisonApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val server = LocationsInsidePrisonApiMockServer()
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

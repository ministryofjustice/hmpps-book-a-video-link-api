package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import java.util.UUID

class LocationsInsidePrisonApiMockServer : MockServer(8091) {

  fun stubPostLocationByKeys(keys: List<String>, prisonId: String = "MDI") {
    keys.forEach { k -> stubGetLocationByKey(k, prisonId) }
  }

  private fun stubGetLocationByKey(key: String, prisonId: String = "MDI") {
    val id = UUID.randomUUID()

    stubFor(
      get("/locations/key/$key").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            mapper.writeValueAsString(
              Location(
                id = id,
                prisonId = prisonId,
                code = "001",
                pathHierarchy = "A-1-001",
                locationType = Location.LocationType.VIDEO_LINK,
                permanentlyInactive = false,
                active = true,
                deactivatedByParent = false,
                topLevelId = id,
                key = key,
                isResidential = true,
              ),
            ),
          )
          .withStatus(200),
      ),
    )
  }

  fun stubGetLocation(locationId: UUID = UUID.randomUUID(), prisonId: String = "MDI") {
    stubFor(
      get("/locations/$locationId").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            mapper.writeValueAsString(
              Location(
                id = locationId,
                prisonId = prisonId,
                code = "001",
                pathHierarchy = "A-1-001",
                locationType = Location.LocationType.VIDEO_LINK,
                permanentlyInactive = false,
                active = true,
                deactivatedByParent = false,
                topLevelId = locationId,
                key = "$prisonId-A-1-001",
                isResidential = true,
              ),
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

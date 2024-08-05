package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoDateTime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.MOORLAND
import java.time.LocalDateTime
import java.util.UUID

class LocationsInsidePrisonApiMockServer : MockServer(8091) {

  fun stubGetLocationByKey(key: String, prisonId: String = "MDI") {
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
                lastModifiedBy = "test user",
                lastModifiedDate = LocalDateTime.now().toIsoDateTime(),
                level = 2,
                leafLevel = true,
                status = Location.Status.ACTIVE,
              ),
            ),
          )
          .withStatus(200),
      ),
    )
  }

  fun stubPostLocationByKeys(keys: Set<String>, prisonCode: String = MOORLAND) {
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
                    prisonId = prisonCode,
                    code = "001",
                    pathHierarchy = "A-1-001",
                    locationType = Location.LocationType.VIDEO_LINK,
                    permanentlyInactive = false,
                    active = true,
                    deactivatedByParent = false,
                    topLevelId = UUID.randomUUID(),
                    key = key,
                    isResidential = true,
                    lastModifiedBy = "test user",
                    lastModifiedDate = LocalDateTime.now().toIsoDateTime(),
                    level = 2,
                    leafLevel = true,
                    status = Location.Status.ACTIVE,
                  )
                },
              ),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubNonResidentialAppointmentLocationsAtPrison(
    keys: Set<String>,
    enabled: Boolean = true,
    prisonCode: String = MOORLAND,
    leafLevel: Boolean = true,
  ) {
    stubFor(
      get("/locations/prison/$prisonCode/non-residential-usage-type/APPOINTMENT")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                keys.map { key ->
                  Location(
                    id = UUID.randomUUID(),
                    prisonId = prisonCode,
                    code = "001",
                    pathHierarchy = "A-1-001",
                    locationType = Location.LocationType.APPOINTMENTS,
                    localName = "$prisonCode $key",
                    permanentlyInactive = false,
                    active = enabled,
                    deactivatedByParent = false,
                    topLevelId = UUID.randomUUID(),
                    key = key,
                    isResidential = false,
                    lastModifiedBy = "test user",
                    lastModifiedDate = LocalDateTime.now().toIsoDateTime(),
                    level = 2,
                    leafLevel = leafLevel,
                    status = Location.Status.ACTIVE,
                  )
                },
              ),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubVideoLinkLocationsAtPrison(
    keys: Set<String>,
    enabled: Boolean = true,
    prisonCode: String = MOORLAND,
    leafLevel: Boolean = true,
  ) {
    stubFor(
      get("/locations/prison/$prisonCode/location-type/VIDEO_LINK")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                keys.map { key ->
                  Location(
                    id = UUID.randomUUID(),
                    prisonId = prisonCode,
                    code = "001",
                    pathHierarchy = "A-1-001",
                    locationType = Location.LocationType.VIDEO_LINK,
                    localName = "$prisonCode $key",
                    permanentlyInactive = false,
                    active = enabled,
                    deactivatedByParent = false,
                    topLevelId = UUID.randomUUID(),
                    key = key,
                    isResidential = false,
                    lastModifiedBy = "test user",
                    lastModifiedDate = LocalDateTime.now().toIsoDateTime(),
                    level = 2,
                    leafLevel = leafLevel,
                    status = Location.Status.ACTIVE,
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

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.LocationKeyValue
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import java.time.LocalDateTime
import java.util.UUID

class LocationsInsidePrisonApiMockServer : MockServer(8091) {

  fun stubGetLocationById(location: Location) {
    stubFor(
      get("/locations/${location.id}?formatLocalName=true").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(mapper.writeValueAsString(location))
          .withStatus(200),
      ),
    )
  }

  fun stubNonResidentialAppointmentLocationsAtPrison(prisonCode: String, vararg locations: Location) {
    stubFor(
      get("/locations/non-residential/prison/$prisonCode/service/APPOINTMENT?sortByLocalName=true&formatLocalName=true&filterParents=true")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(locations))
            .withStatus(200),
        ),
    )
  }

  fun stubNonResidentialAppointmentLocationsAtPrison(
    keys: List<LocationKeyValue>,
    enabled: Boolean = true,
    prisonCode: String = WANDSWORTH,
    leafLevel: Boolean = true,
  ) {
    stubFor(
      get("/locations/non-residential/prison/$prisonCode/service/APPOINTMENT?sortByLocalName=true&formatLocalName=true&filterParents=true")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                keys.map {
                  Location(
                    id = it.id,
                    prisonId = prisonCode,
                    code = "001",
                    pathHierarchy = "A-1-001",
                    locationType = Location.LocationType.APPOINTMENTS,
                    localName = "$prisonCode ${it.key}",
                    permanentlyInactive = false,
                    active = enabled,
                    deactivatedByParent = false,
                    topLevelId = UUID.randomUUID(),
                    key = it.key,
                    isResidential = false,
                    lastModifiedBy = "test user",
                    lastModifiedDate = LocalDateTime.now(),
                    level = 2,
                    leafLevel = leafLevel,
                    status = if (enabled) Location.Status.ACTIVE else Location.Status.INACTIVE,
                    locked = false,
                    inactiveStatus = null,
                  )
                },
              ),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubVideoLinkLocationsAtPrison(
    keys: List<LocationKeyValue>,
    enabled: Boolean = true,
    prisonCode: String = WANDSWORTH,
    leafLevel: Boolean = true,
  ) {
    stubFor(
      get("/locations/non-residential/prison/$prisonCode/service/VIDEO_LINK?sortByLocalName=true&formatLocalName=true&filterParents=true")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                keys.map {
                  Location(
                    id = it.id,
                    prisonId = prisonCode,
                    code = "001",
                    pathHierarchy = "A-1-001",
                    locationType = Location.LocationType.VIDEO_LINK,
                    localName = "$prisonCode ${it.key}",
                    permanentlyInactive = false,
                    active = enabled,
                    deactivatedByParent = false,
                    topLevelId = UUID.randomUUID(),
                    key = it.key,
                    isResidential = false,
                    lastModifiedBy = "test user",
                    lastModifiedDate = LocalDateTime.now(),
                    level = 2,
                    leafLevel = leafLevel,
                    status = if (enabled) Location.Status.ACTIVE else Location.Status.INACTIVE,
                    locked = false,
                    inactiveStatus = null,
                  )
                },
              ),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubVideoLinkLocationsAtPrison(
    prisonCode: String = WANDSWORTH,
    vararg locations: Location,
  ) {
    stubFor(
      get("/locations/non-residential/prison/$prisonCode/service/VIDEO_LINK?sortByLocalName=true&formatLocalName=true&filterParents=true")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(locations))
            .withStatus(200),
        ),
    )
  }
}

class LocationsInsidePrisonApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
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

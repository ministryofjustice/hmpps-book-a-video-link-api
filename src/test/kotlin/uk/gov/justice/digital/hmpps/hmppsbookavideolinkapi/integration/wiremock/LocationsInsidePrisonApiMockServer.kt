package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import java.util.UUID

class LocationsInsidePrisonApiMockServer : WireMockServer(8091) {

  val mapper: ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

  fun stubHealthPing(status: Int) {
    stubFor(
      get("/health/ping").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(if (status == 200) """{"status":"UP"}""" else """{"status":"DOWN"}""")
          .withStatus(status),
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

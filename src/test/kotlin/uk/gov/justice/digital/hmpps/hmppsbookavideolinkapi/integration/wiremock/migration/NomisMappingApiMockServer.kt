package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.migration

import com.github.tomakehurst.wiremock.client.WireMock
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.nomismapping.NomisDpsLocationMapping
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.MockServer
import java.util.UUID

@Deprecated(message = "Can be removed when migration is completed")
class NomisMappingApiMockServer : MockServer(8096) {

  @Suppress("DeprecatedCallableAddReplaceWith")
  @Deprecated(message = "Can be removed when migration is completed")
  fun stubGetNomisToDpsLocationMapping(internalLocationId: Long, dpsLocationId: UUID) {
    stubFor(
      WireMock.get("/api/locations/nomis/$internalLocationId")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(NomisDpsLocationMapping(dpsLocationId, internalLocationId)))
            .withStatus(200),
        ),
    )
  }
}

@Deprecated(message = "Can be removed when migration is completed")
class NomisMappingApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
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

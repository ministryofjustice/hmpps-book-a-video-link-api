package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.migration

import com.github.tomakehurst.wiremock.client.WireMock
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.VideoBookingMigrateResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.MockServer

@Deprecated(message = "Can be removed when migration is completed")
class WhereaboutsApiMockServer : MockServer(8095) {

  fun stubGetVideoBookingToMigrate(videoBookingId: Long, bookingToMigrate: VideoBookingMigrateResponse) {
    stubFor(
      WireMock.get("/migrate/video-link-booking/$videoBookingId")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(bookingToMigrate))
            .withStatus(200),
        ),
    )
  }
}

@Deprecated(message = "Can be removed when migration is completed")
class WhereaboutsApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val server = WhereaboutsApiMockServer()
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

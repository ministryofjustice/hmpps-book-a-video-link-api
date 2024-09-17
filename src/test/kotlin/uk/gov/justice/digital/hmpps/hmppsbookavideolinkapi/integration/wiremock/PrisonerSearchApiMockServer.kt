package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.BookingIds
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisonerSearchPrisoner

class PrisonerSearchApiMockServer : MockServer(8092) {

  @Suppress("DeprecatedCallableAddReplaceWith")
  @Deprecated(message = "Can be removed when migration is completed")
  fun stubPostGetPrisonerByBookingId(bookingId: Long, prisonerNumber: String, prisonCode: String) {
    stubFor(
      WireMock.post("/prisoner-search/booking-ids")
        .withRequestBody(WireMock.equalToJson(mapper.writeValueAsString(BookingIds(listOf(bookingId)))))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                listOf(
                  prisonerSearchPrisoner(
                    prisonerNumber = prisonerNumber,
                    prisonCode = prisonCode,
                    lastPrisonCode = null,
                  ),
                ),
              ),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubGetPrisoner(prisonerNumber: String, prisonCode: String = "MDI", lastPrisonCode: String? = null) {
    stubFor(
      get("/prisoner/$prisonerNumber")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                prisonerSearchPrisoner(
                  prisonerNumber = prisonerNumber,
                  prisonCode = prisonCode,
                  lastPrisonCode = lastPrisonCode,
                ),
              ),
            )
            .withStatus(200),
        ),
    )
  }
}

class PrisonerSearchApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val server = PrisonerSearchApiMockServer()
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

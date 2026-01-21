package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerNumbers
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisonerSearchPrisoner

class PrisonerSearchApiMockServer : MockServer(8092) {

  fun stubGetPrisoner(prisoner: Prisoner) = stubGetPrisoner(prisoner.number, prisoner.prison)

  fun stubGetPrisoner(prisonerNumber: String, prisonCode: String = "WWI", lastPrisonCode: String? = null) {
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

  fun stubSearchPrisonersByPrisonerNumbers(prisoners: List<Prisoner>) {
    stubFor(
      post("/prisoner-search/prisoner-numbers")
        .withRequestBody(equalToJson(mapper.writeValueAsString(PrisonerNumbers(prisoners.map { it.number })), true, true))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(prisoners.map { prisonerSearchPrisoner(prisonCode = it.prison, prisonerNumber = it.number) }))
            .withStatus(200),
        ),
    )
  }
}

class PrisonerSearchApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
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

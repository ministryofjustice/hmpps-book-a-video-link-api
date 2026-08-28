package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.officialvisits.model.OfficialVisitSummarySearchRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.officialvisits.model.OfficialVisitSummarySearchResponse
import java.time.LocalDate

class OfficialVisitsApiMockServer : MockServer(8097) {
  fun stubPostFindOfficialVisitsBy(prisonCode: String, startDate: LocalDate, endDate: LocalDate, vararg response: OfficialVisitSummarySearchResponse) {
    stubFor(
      post(urlPathEqualTo("/official-visit/prison/$prisonCode/find-by-criteria"))
        .withRequestBody(
          equalToJson(mapper.writeValueAsString(OfficialVisitSummarySearchRequest(startDate = startDate, endDate = endDate))),
        )
        .withQueryParam("page", equalTo("0"))
        .withQueryParam("size", equalTo("400"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(PagedResponse(response.toList())))
            .withStatus(200),
        ),
    )
  }

  data class PagedResponse(
    val content: List<OfficialVisitSummarySearchResponse>,
    val page: Int = 0,
    val size: Int = 100,
    val totalElements: Int = content.size,
    val totalPages: Int = 0,
  )
}

class OfficialVisitsApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val server = OfficialVisitsApiMockServer()
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

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.http.HttpHeader
import com.github.tomakehurst.wiremock.http.HttpHeaders
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class HmppsAuthMockServer : MockServer(8090, "/auth") {

  fun stubGrantToken() {
    stubFor(
      post(urlEqualTo("/auth/oauth/token"))
        .willReturn(
          aResponse()
            .withHeaders(HttpHeaders(HttpHeader("Content-Type", "application/json")))
            .withBody(
              """
              {
                 "access_token": "ABCDE", 
                 "token_type": "bearer"
              }
              """.trimIndent(),
            ),
        ),
    )
  }
}

class HmppsAuthApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
  companion object {
    @JvmField
    val server = HmppsAuthMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    server.start()
    server.stubGrantToken()
  }

  override fun beforeEach(context: ExtensionContext) {
    server.resetAll()
    server.stubGrantToken()
  }

  override fun afterAll(context: ExtensionContext) {
    server.stop()
  }
}

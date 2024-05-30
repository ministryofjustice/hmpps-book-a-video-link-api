package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.userDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.userEmail

class ManageUsersApiMockServer : MockServer(8093) {

  fun stubGetUserDetails(username: String = TEST_USERNAME, name: String) {
    stubFor(
      get("/users/$username")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(userDetails(username, name)))
            .withStatus(200),
        ),
    )
  }

  fun stubGetUserEmail(username: String = TEST_USERNAME, email: String = TEST_USER_EMAIL) {
    stubFor(
      get("/users/$username/email")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(userEmail(username, email)))
            .withStatus(200),
        ),
    )
  }
}

class ManageUsersApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val server = ManageUsersApiMockServer()
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

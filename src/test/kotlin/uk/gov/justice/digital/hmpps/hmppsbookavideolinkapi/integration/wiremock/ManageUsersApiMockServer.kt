package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.model.UserDetailsDto.AuthSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.userDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.userEmailAddress
import java.net.URLEncoder

class ManageUsersApiMockServer : MockServer(8093) {

  fun stubGetUserDetails(username: String = TEST_EXTERNAL_USER, authSource: AuthSource = AuthSource.auth, name: String) {
    stubFor(
      get("/users/${username.urlEncode()}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(userDetails(username, name, authSource)))
            .withStatus(200),
        ),
    )
  }

  fun stubGetUserEmail(username: String = TEST_EXTERNAL_USER, email: String = TEST_EXTERNAL_USER_EMAIL, verified: Boolean = true) {
    stubFor(
      get("/users/${username.urlEncode()}/email?unverified=false")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(userEmailAddress(username, email, verified)))
            .withStatus(200),
        ),
    )
  }

  private fun String.urlEncode() = URLEncoder.encode(this, "utf-8")
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

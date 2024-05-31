package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.userDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.userEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.ManageUsersApiMockServer

class ManageUsersClientTest {

  private val server = ManageUsersApiMockServer().also { it.start() }
  private val client = ManageUsersClient(WebClient.create("http://localhost:${server.port()}"))

  @Test
  fun `should get users details`() {
    server.stubGetUserDetails("username", "name")

    client.getUsersDetails("username") isEqualTo userDetails("username", "name")
  }

  @Test
  fun `should get users email`() {
    server.stubGetUserEmail("username", "email")

    client.getUsersEmail("username") isEqualTo userEmail("username", "email")
  }

  @AfterEach
  fun after() {
    server.stop()
  }
}

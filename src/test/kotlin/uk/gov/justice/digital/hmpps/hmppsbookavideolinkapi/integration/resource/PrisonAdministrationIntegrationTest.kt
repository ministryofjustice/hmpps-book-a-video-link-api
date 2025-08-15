package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendPrisonRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser

@Sql("classpath:integration-test-data/clean-test-prison.sql", "classpath:integration-test-data/seed-test-prison.sql")
class PrisonAdministrationIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var prisonRepository: PrisonRepository

  @Test
  fun `should update test prison pick-up time`() {
    prisonRepository.findByCode("TEST")!!.pickUpTime isEqualTo 30

    webTestClient.amendPrison("TEST", AmendPrisonRequest(pickUpTime = 60), COURT_USER)

    prisonRepository.findByCode("TEST")!!.pickUpTime isEqualTo 60
  }

  private fun WebTestClient.amendPrison(prisonCode: String, request: AmendPrisonRequest, user: ExternalUser) = this
    .put()
    .uri("/prison-admin/$prisonCode")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(user = user.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(Prison::class.java)
    .returnResult().responseBody!!
}

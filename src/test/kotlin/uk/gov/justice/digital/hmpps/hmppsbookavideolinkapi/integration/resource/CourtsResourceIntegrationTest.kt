package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository

class CourtsResourceIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var courtRepository: CourtRepository

  @Sql("classpath:integration-test-data/clean-enabled-court-data.sql")
  @AfterEach
  fun afterEach() {
  }

  @Sql("classpath:integration-test-data/seed-enabled-court-data.sql")
  @Test
  fun `should return a list of enabled courts`() {
    courtRepository.findAll() hasSize 3

    val listOfEnabledCourts = webTestClient.getEnabledCourts()

    assertThat(listOfEnabledCourts).hasSize(2)
    assertThat(listOfEnabledCourts).extracting("code").contains("ENABLED")
    assertThat(listOfEnabledCourts).extracting("code").doesNotContain("NOT_ENABLED")
  }

  private fun WebTestClient.getEnabledCourts() =
    get()
      .uri("/courts/enabled")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(Court::class.java)
      .returnResult().responseBody
}

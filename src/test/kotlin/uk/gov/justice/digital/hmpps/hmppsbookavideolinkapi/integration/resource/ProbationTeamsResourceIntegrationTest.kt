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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.ProbationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository

class ProbationTeamsResourceIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var probationTeamRepository: ProbationTeamRepository

  @Sql("classpath:integration-test-data/clean-enabled-probation-team-data.sql")
  @AfterEach
  fun afterEach() {
  }

  @Sql("classpath:integration-test-data/seed-enabled-probation-team-data.sql")
  @Test
  fun `should return a list of enabled probation teams`() {
    probationTeamRepository.findAll() hasSize 3

    val listOfEnabledTeams = webTestClient.getEnabledProbationTeams()

    assertThat(listOfEnabledTeams).hasSize(2)
    assertThat(listOfEnabledTeams).extracting("code").contains("ENABLED")
    assertThat(listOfEnabledTeams).extracting("code").doesNotContain("NOT_ENABLED")
  }

  private fun WebTestClient.getEnabledProbationTeams() =
    get()
      .uri("/probation-teams/enabled")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ProbationTeam::class.java)
      .returnResult().responseBody
}

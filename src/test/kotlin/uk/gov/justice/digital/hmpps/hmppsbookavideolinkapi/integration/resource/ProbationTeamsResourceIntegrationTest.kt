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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.SetProbationTeamPreferencesRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.SetProbationTeamPreferencesResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.UserProbationRepository

class ProbationTeamsResourceIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var probationTeamRepository: ProbationTeamRepository

  @Autowired
  private lateinit var userProbationRepository: UserProbationRepository

  @Sql(
    "classpath:integration-test-data/clean-enabled-probation-team-data.sql",
    "classpath:integration-test-data/clean-user-probation-team-data.sql",
  )
  @AfterEach
  fun afterEach() {
  }

  @Sql("classpath:integration-test-data/seed-enabled-probation-team-data.sql")
  @Test
  fun `should return a list of enabled probation teams`() {
    probationTeamRepository.findAll() hasSize 30

    val listOfEnabledTeams = webTestClient.getEnabledProbationTeams()

    assertThat(listOfEnabledTeams).extracting("code").contains("ENABLED")
    assertThat(listOfEnabledTeams).extracting("code").doesNotContain("NOT_ENABLED")
  }

  @Sql("classpath:integration-test-data/seed-user-probation-team-data.sql")
  @Test
  fun `should return a list of preferred probation teams for a specified user`() {
    val listOfPreferredTeams = webTestClient.getUserPreferenceTeams("michael.horden@channel4.com")

    // Check that the user-preferences as setup by the SQL above are returned
    assertThat(listOfPreferredTeams).extracting("probationTeamId").containsExactlyInAnyOrder(1L, 2L)
    assertThat(listOfPreferredTeams).extracting("code").containsExactlyInAnyOrder("BLKPPP", "BARSPP")

    // And that it avoids the preferences set for other users
    assertThat(listOfPreferredTeams).extracting("probationTeamId").doesNotContain(3L)
    assertThat(listOfPreferredTeams).extracting("code").doesNotContain("PPOCFD")
  }

  @Test
  fun `should set a list of preferred probation teams for a specified user`() {
    val newTeams = listOf("BLKPPP", "BARSPP", "PPOCFD")
    val username = "test-user@mymail.com"
    val request = SetProbationTeamPreferencesRequest(probationTeamCodes = newTeams)

    userProbationRepository.findAll() hasSize 0

    val response = webTestClient.setUserPreferenceTeams(request, username)

    assertThat(response?.probationTeamsSaved).isEqualTo(3)
    userProbationRepository.findAll() hasSize 3

    val listOfPreferredTeams = webTestClient.getUserPreferenceTeams(username)
    assertThat(listOfPreferredTeams).extracting("code").containsAll(newTeams)

    userProbationRepository.deleteAll()
  }

  @Test
  fun `should replace the preferred teams for a specified user`() {
    val teams1 = listOf("BLKPPP", "BARSPP", "PPOCFD")
    val username = "test-user@mymail.com"
    val request1 = SetProbationTeamPreferencesRequest(probationTeamCodes = teams1)

    val response = webTestClient.setUserPreferenceTeams(request1, username)
    assertThat(response?.probationTeamsSaved).isEqualTo(3)

    userProbationRepository.findAll() hasSize 3

    val listOfPreferredTeams = webTestClient.getUserPreferenceTeams(username)
    assertThat(listOfPreferredTeams).extracting("code").containsAll(teams1)

    // Replace originals with this set of different teams
    val teams2 = listOf("PRESPC", "PRESPM", "BURNPC")
    val request2 = SetProbationTeamPreferencesRequest(probationTeamCodes = teams2)

    webTestClient.setUserPreferenceTeams(request2, username)

    val newPreferredTeams = webTestClient.getUserPreferenceTeams(username)
    assertThat(newPreferredTeams).extracting("code").containsAll(teams2)

    userProbationRepository.findAll() hasSize 3
    userProbationRepository.deleteAll()
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

  private fun WebTestClient.getUserPreferenceTeams(username: String) =
    get()
      .uri("/probation-teams/user-preferences/{username}", username)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ProbationTeam::class.java)
      .returnResult().responseBody

  private fun WebTestClient.setUserPreferenceTeams(
    request: SetProbationTeamPreferencesRequest,
    username: String,
  ) =
    post()
      .uri("/probation-teams/user-preferences/set")
      .bodyValue(request)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SetProbationTeamPreferencesResponse::class.java)
      .returnResult().responseBody
}

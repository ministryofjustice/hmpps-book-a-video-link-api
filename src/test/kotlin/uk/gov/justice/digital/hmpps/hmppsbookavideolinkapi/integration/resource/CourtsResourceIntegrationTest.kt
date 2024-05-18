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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.SetCourtPreferencesRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.SetCourtPreferencesResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.UserCourtRepository

class CourtsResourceIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var courtRepository: CourtRepository

  @Autowired
  private lateinit var userCourtRepository: UserCourtRepository

  @Sql(
    "classpath:integration-test-data/clean-enabled-court-data.sql",
    "classpath:integration-test-data/clean-user-court-data.sql",
  )
  @AfterEach
  fun afterEach() {
  }

  @Sql("classpath:integration-test-data/seed-enabled-court-data.sql")
  @Test
  fun `should return a list of enabled courts`() {
    courtRepository.findAll() hasSize 328

    val listOfEnabledCourts = webTestClient.getEnabledCourts()

    assertThat(listOfEnabledCourts).extracting("code").contains("ENABLED")
    assertThat(listOfEnabledCourts).extracting("code").doesNotContain("NOT_ENABLED")
  }

  @Sql("classpath:integration-test-data/seed-user-court-data.sql")
  @Test
  fun `should return a list of preferred courts for a specified user`() {
    val listOfPreferredCourts = webTestClient.getUserPreferenceCourts("michael.horden@itv.com")

    // Check that the user-preferences as setup by the SQL above are returned
    assertThat(listOfPreferredCourts).extracting("courtId").containsExactlyInAnyOrder(1L, 2L)
    assertThat(listOfPreferredCourts).extracting("code").containsExactlyInAnyOrder("DRBYMC", "NWPIAC")

    // And that the trixy hoax values for a different username are not
    assertThat(listOfPreferredCourts).extracting("courtId").doesNotContain(3L)
    assertThat(listOfPreferredCourts).extracting("code").doesNotContain("CRNRCT")
  }

  @Test
  fun `should set a list of preferred courts for a specified user`() {
    val newCourts = listOf("CVNTCC", "DRBYCC", "MNSFMC")
    val username = "test-user@mymail.com"
    val request = SetCourtPreferencesRequest(courtCodes = newCourts)

    userCourtRepository.findAll() hasSize 0

    val response = webTestClient.setUserPreferenceCourts(request, username)

    assertThat(response?.courtsSaved).isEqualTo(3)
    userCourtRepository.findAll() hasSize 3

    val listOfPreferredCourts = webTestClient.getUserPreferenceCourts(username)
    assertThat(listOfPreferredCourts).extracting("code").containsAll(newCourts)

    userCourtRepository.deleteAll()
  }

  @Test
  fun `should replace the preferred courts for a specified user`() {
    val courts1 = listOf("CVNTCC", "DRBYCC", "MNSFMC")
    val username = "test-user@mymail.com"
    val request1 = SetCourtPreferencesRequest(courtCodes = courts1)

    val response = webTestClient.setUserPreferenceCourts(request1, username)
    assertThat(response?.courtsSaved).isEqualTo(3)

    userCourtRepository.findAll() hasSize 3

    val listOfPreferredCourts = webTestClient.getUserPreferenceCourts(username)
    assertThat(listOfPreferredCourts).extracting("code").containsAll(courts1)

    // Replace originals with this set of different courts
    val courts2 = listOf("SWINCC", "SWINMC", "AMERCC")
    val request2 = SetCourtPreferencesRequest(courtCodes = courts2)

    webTestClient.setUserPreferenceCourts(request2, username)

    val newPreferredCourts = webTestClient.getUserPreferenceCourts(username)
    assertThat(newPreferredCourts).extracting("code").containsAll(courts2)

    userCourtRepository.findAll() hasSize 3
    userCourtRepository.deleteAll()
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

  private fun WebTestClient.getUserPreferenceCourts(username: String) =
    get()
      .uri("/courts/user-preferences/{username}", username)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(Court::class.java)
      .returnResult().responseBody

  private fun WebTestClient.setUserPreferenceCourts(
    request: SetCourtPreferencesRequest,
    username: String,
  ) =
    post()
      .uri("/courts/user-preferences/set")
      .bodyValue(request)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SetCourtPreferencesResponse::class.java)
      .returnResult().responseBody
}

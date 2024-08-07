package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.model.UserDetailsDto.AuthSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.ActivitiesAppointmentsApiExtension
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.LocationsInsidePrisonApiExtension
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.ManageUsersApiExtension
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.PrisonApiExtension
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.PrisonerSearchApiExtension
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.TEST_EXTERNAL_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.TEST_EXTERNAL_USER_EMAIL
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.TEST_PRISON_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.TEST_PRISON_USER_EMAIL
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserType

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(
  ActivitiesAppointmentsApiExtension::class,
  HmppsAuthApiExtension::class,
  LocationsInsidePrisonApiExtension::class,
  ManageUsersApiExtension::class,
  PrisonApiExtension::class,
  PrisonerSearchApiExtension::class,
)
@Sql(
  "classpath:test_data/clean-all-data.sql",
)
abstract class IntegrationTestBase {

  @Autowired
  protected lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  protected fun setAuthorisation(
    user: String = TEST_EXTERNAL_USER,
    roles: List<String> = listOf(),
    scopes: List<String> = listOf("read"),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles, scopes)

  protected fun stubPingWithResponse(status: Int) {
    ActivitiesAppointmentsApiExtension.server.stubHealthPing(status)
    HmppsAuthApiExtension.server.stubHealthPing(status)
    locationsInsidePrisonApi().stubHealthPing(status)
    manageUsersApi().stubHealthPing(status)
    prisonerApi().stubHealthPing(status)
    prisonSearchApi().stubHealthPing(status)
  }

  protected fun prisonerApi() = PrisonApiExtension.server
  protected fun prisonSearchApi() = PrisonerSearchApiExtension.server

  protected fun locationsInsidePrisonApi() = LocationsInsidePrisonApiExtension.server

  protected fun manageUsersApi() = ManageUsersApiExtension.server

  protected fun stubUser(username: String = TEST_EXTERNAL_USER, name: String = "Test Users Name", userType: UserType = UserType.EXTERNAL, email: String? = TEST_EXTERNAL_USER_EMAIL) {
    val authSource = when (userType) {
      UserType.EXTERNAL -> AuthSource.auth
      UserType.PRISON -> AuthSource.nomis
      else -> AuthSource.none
    }
    manageUsersApi().stubGetUserDetails(username, authSource, name)
    if (email != null) manageUsersApi().stubGetUserEmail(username, email)
  }

  @BeforeEach
  fun `stub user`() {
    stubUser(TEST_EXTERNAL_USER, userType = UserType.EXTERNAL, email = TEST_EXTERNAL_USER_EMAIL)
    stubUser(TEST_PRISON_USER, userType = UserType.PRISON, email = TEST_PRISON_USER_EMAIL)
  }
}

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.model.UserDetailsDto.AuthSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.ActivitiesAppointmentsApiExtension
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.LocationsInsidePrisonApiExtension
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.ManageUsersApiExtension
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.PrisonApiExtension
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.PrisonerSearchApiExtension
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.migration.NomisMappingApiExtension
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.migration.WhereaboutsApiExtension
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User
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
  WhereaboutsApiExtension::class,
  NomisMappingApiExtension::class,
)
@Sql(
  "classpath:test_data/clean-all-data.sql",
)
abstract class IntegrationTestBase {

  @Autowired
  protected lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthHelper

  @BeforeEach
  fun `stub default users`() {
    stubUser(PRISON_USER_BIRMINGHAM)
    stubUser(COURT_USER)
    stubUser(PROBATION_USER)
  }

  protected fun setAuthorisation(
    user: String = "AUTH_ADM",
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
    whereaboutsApi().stubHealthPing(status)
    nomisMappingApi().stubHealthPing(status)
  }

  protected fun prisonerApi() = PrisonApiExtension.server
  protected fun prisonSearchApi() = PrisonerSearchApiExtension.server

  protected fun locationsInsidePrisonApi() = LocationsInsidePrisonApiExtension.server

  protected fun manageUsersApi() = ManageUsersApiExtension.server

  protected fun whereaboutsApi() = WhereaboutsApiExtension.server

  protected fun nomisMappingApi() = NomisMappingApiExtension.server

  protected fun stubUser(user: User) {
    val authSource = when {
      user.isUserType(UserType.EXTERNAL) -> AuthSource.auth
      user.isUserType(UserType.PRISON) -> AuthSource.nomis
      else -> AuthSource.none
    }

    val userId = when {
      user.isUserType(UserType.EXTERNAL) -> "external"
      user.isUserType(UserType.PRISON) -> "nomis"
      else -> "other"
    }

    val mayBeActiveCaseload = if (user is PrisonUser) user.activeCaseLoadId else null

    manageUsersApi().stubGetUserDetails(user.username, authSource, user.name, mayBeActiveCaseload, userId)
    manageUsersApi().stubGetUserGroups(userId)

    when (user) {
      is ExternalUser -> user.email
      is PrisonUser -> user.email
      else -> null
    }?.let { email -> manageUsersApi().stubGetUserEmail(user.username, email) }
  }

  protected fun WebTestClient.createBooking(request: CreateVideoBookingRequest, user: User) =
    this
      .post()
      .uri("/video-link-booking")
      .bodyValue(request)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = user.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Long::class.java)
      .returnResult().responseBody!!
}

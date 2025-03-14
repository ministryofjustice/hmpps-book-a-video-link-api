package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.model.UserDetailsDto.AuthSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.norwichLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvilleLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.risleyLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.container.PostgresContainer
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.ActivitiesAppointmentsApiExtension
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.LocationsInsidePrisonApiExtension
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.ManageUsersApiExtension
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.NomisMappingApiExtension
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.PrisonApiExtension
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.PrisonerSearchApiExtension
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(
  ActivitiesAppointmentsApiExtension::class,
  HmppsAuthApiExtension::class,
  LocationsInsidePrisonApiExtension::class,
  ManageUsersApiExtension::class,
  PrisonApiExtension::class,
  PrisonerSearchApiExtension::class,
  NomisMappingApiExtension::class,
)
@Sql(
  "classpath:test_data/clean-all-data.sql",
  "classpath:test_data/seed-contacts.sql",
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

  @BeforeEach
  fun `stub default locations`() {
    setOf(birminghamLocation, norwichLocation, pentonvilleLocation, risleyLocation, wandsworthLocation).forEach {
      locationsInsidePrisonApi().stubGetLocationByKey(it)
      locationsInsidePrisonApi().stubPostLocationByKeys(it)
      locationsInsidePrisonApi().stubGetLocationById(it)
    }
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
    prisonApi().stubHealthPing(status)
    prisonSearchApi().stubHealthPing(status)
    nomisMappingApi().stubHealthPing(status)
  }

  protected fun activitiesAppointmentsApi() = ActivitiesAppointmentsApiExtension.server
  protected fun prisonApi() = PrisonApiExtension.server

  protected fun prisonSearchApi() = PrisonerSearchApiExtension.server

  protected fun locationsInsidePrisonApi() = LocationsInsidePrisonApiExtension.server

  protected fun manageUsersApi() = ManageUsersApiExtension.server

  protected fun nomisMappingApi() = NomisMappingApiExtension.server

  protected fun stubUser(user: User) {
    val authSource = when (user) {
      is ExternalUser -> AuthSource.auth
      is PrisonUser -> AuthSource.nomis
      else -> AuthSource.none
    }

    val userId = when (user) {
      is ExternalUser -> "external"
      is PrisonUser -> "nomis"
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

  protected fun WebTestClient.createBooking(request: CreateVideoBookingRequest, user: User) = this
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

  companion object {
    private val pgContainer = PostgresContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun properties(registry: DynamicPropertyRegistry) {
      pgContainer?.run {
        registry.add("spring.datasource.url", pgContainer::getJdbcUrl)
        registry.add("spring.datasource.username", pgContainer::getUsername)
        registry.add("spring.datasource.password", pgContainer::getPassword)
      }
    }
  }
}

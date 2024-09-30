package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.model.UserDetailsDto.AuthSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER
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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.AmendedCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.AmendedCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.AmendedCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.AmendedCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CancelledCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CancelledCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CancelledCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CancelledCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CourtBookingRequestPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CourtBookingRequestPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CourtBookingRequestUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CourtHearingLinkReminderEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.NewCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.NewCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.NewCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.NewCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.AmendedProbationBookingPrisonNoProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.AmendedProbationBookingPrisonProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.AmendedProbationBookingProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.AmendedProbationBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.CancelledProbationBookingPrisonNoProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.CancelledProbationBookingPrisonProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.CancelledProbationBookingProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.CancelledProbationBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.NewProbationBookingPrisonNoProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.NewProbationBookingPrisonProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.NewProbationBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ProbationBookingRequestPrisonNoProbationTeamEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ProbationBookingRequestPrisonProbationTeamEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ProbationBookingRequestUserEmail
import java.util.UUID

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
    stubUser(PRISON_USER)
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

    manageUsersApi().stubGetUserDetails(user.username, authSource, user.name, user.activeCaseLoadId, userId)
    manageUsersApi().stubGetUserGroups(userId)
    user.email?.let { manageUsersApi().stubGetUserEmail(user.username, it) }
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

@TestConfiguration
class TestEmailConfiguration {
  @Bean
  fun emailService() =
    EmailService { email ->
      when (email) {
        is NewCourtBookingUserEmail -> Result.success(UUID.randomUUID() to "new court booking user template id")
        is NewCourtBookingCourtEmail -> Result.success(UUID.randomUUID() to "new court booking court template id")
        is NewCourtBookingPrisonCourtEmail -> Result.success(UUID.randomUUID() to "new court booking prison template id with email address")
        is NewCourtBookingPrisonNoCourtEmail -> Result.success(UUID.randomUUID() to "new court booking prison template id no email address")
        is AmendedCourtBookingUserEmail -> Result.success(UUID.randomUUID() to "amended court booking user template id")
        is AmendedCourtBookingCourtEmail -> Result.success(UUID.randomUUID() to "amended court booking court template id")
        is AmendedCourtBookingPrisonCourtEmail -> Result.success(UUID.randomUUID() to "amended court booking prison template id with email address")
        is AmendedCourtBookingPrisonNoCourtEmail -> Result.success(UUID.randomUUID() to "amended court booking prison template id no email address")
        is CancelledCourtBookingUserEmail -> Result.success(UUID.randomUUID() to "cancelled court booking user template id")
        is CancelledCourtBookingCourtEmail -> Result.success(UUID.randomUUID() to "cancelled court booking user template id")
        is CancelledCourtBookingPrisonCourtEmail -> Result.success(UUID.randomUUID() to "cancelled court booking prison template id with email address")
        is CancelledCourtBookingPrisonNoCourtEmail -> Result.success(UUID.randomUUID() to "cancelled court booking prison template id no email address")
        is CourtBookingRequestUserEmail -> Result.success(UUID.randomUUID() to "requested court booking user template id")
        is CourtBookingRequestPrisonCourtEmail -> Result.success(UUID.randomUUID() to "requested court booking prison template id with email address")
        is CourtBookingRequestPrisonNoCourtEmail -> Result.success(UUID.randomUUID() to "requested court booking prison template id with no email address")
        is ProbationBookingRequestUserEmail -> Result.success(UUID.randomUUID() to "requested probation booking user template id")
        is ProbationBookingRequestPrisonProbationTeamEmail -> Result.success(UUID.randomUUID() to "requested probation booking prison template id with email address")
        is ProbationBookingRequestPrisonNoProbationTeamEmail -> Result.success(UUID.randomUUID() to "requested probation booking prison template id with no email address")
        is NewProbationBookingUserEmail -> Result.success(UUID.randomUUID() to "new probation booking user template id")
        is NewProbationBookingPrisonProbationEmail -> Result.success(UUID.randomUUID() to "new probation booking prison template id with email address")
        is NewProbationBookingPrisonNoProbationEmail -> Result.success(UUID.randomUUID() to "new probation booking prison template id no email address")
        is AmendedProbationBookingUserEmail -> Result.success(UUID.randomUUID() to "amended probation booking user template id")
        is AmendedProbationBookingPrisonProbationEmail -> Result.success(UUID.randomUUID() to "amended probation booking template id with email address")
        is AmendedProbationBookingPrisonNoProbationEmail -> Result.success(UUID.randomUUID() to "amended probation booking template id no email address")
        is AmendedProbationBookingProbationEmail -> Result.success(UUID.randomUUID() to "amended probation booking probation template id")
        is CancelledProbationBookingUserEmail -> Result.success(UUID.randomUUID() to "cancelled probation booking user template id")
        is CancelledProbationBookingProbationEmail -> Result.success(UUID.randomUUID() to "cancelled probation booking probation template id")
        is CancelledProbationBookingPrisonProbationEmail -> Result.success(UUID.randomUUID() to "cancelled probation booking template id with email address")
        is CancelledProbationBookingPrisonNoProbationEmail -> Result.success(UUID.randomUUID() to "cancelled probation booking template id no email address")
        is CourtHearingLinkReminderEmail -> Result.success(UUID.randomUUID() to "court hearing link reminder template id")
        else -> throw RuntimeException("Unsupported email in test email configuration: ${email.javaClass.simpleName}")
      }
    }
}

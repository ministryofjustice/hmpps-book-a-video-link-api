package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Notification
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.StatusCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BLACKPOOL_MC_PPOC
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.CHESTERFIELD_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WERRINGTON
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendCourtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendProbationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.werringtonLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.TEST_USERNAME
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.TEST_USER_EMAIL
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.VideoLinkBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.AmendedCourtBookingEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.AmendedCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.AmendedCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.CancelledCourtBookingEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.CancelledCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.CancelledCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NewCourtBookingEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NewCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NewCourtBookingPrisonNoCourtEmail
import java.time.LocalTime
import java.util.UUID

@ContextConfiguration(classes = [TestEmailConfiguration::class])
class VideoLinkBookingIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var videoBookingRepository: VideoBookingRepository

  @Autowired
  private lateinit var prisonAppointmentRepository: PrisonAppointmentRepository

  @Autowired
  private lateinit var notificationRepository: NotificationRepository

  @Test
  fun `should create a Derby court booking and emails sent to Werrington prison`() {
    videoBookingRepository.findAll() hasSize 0
    notificationRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", WERRINGTON)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(werringtonLocation.key), WERRINGTON)
    manageUsersApi().stubGetUserDetails(TEST_USERNAME, "Test Users Name")
    manageUsersApi().stubGetUserEmail(TEST_USERNAME, TEST_USER_EMAIL)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = WERRINGTON,
      location = werringtonLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, TEST_USERNAME)

    val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    with(persistedBooking) {
      videoBookingId isEqualTo bookingId
      bookingType isEqualTo "COURT"
      court?.code isEqualTo courtBookingRequest.courtCode
      hearingType isEqualTo courtBookingRequest.courtHearingType?.name
      comments isEqualTo "integration test court booking comments"
      videoUrl isEqualTo courtBookingRequest.videoLinkUrl
      createdBy isEqualTo TEST_USERNAME
      createdByPrison isEqualTo false
    }

    with(prisonAppointmentRepository.findByVideoBooking(persistedBooking).single()) {
      videoBooking isEqualTo persistedBooking
      prisonCode isEqualTo WERRINGTON
      prisonerNumber isEqualTo "123456"
      appointmentType isEqualTo AppointmentType.VLB_COURT_MAIN.name
      appointmentDate isEqualTo tomorrow()
      prisonLocKey isEqualTo werringtonLocation.key
      startTime isEqualTo LocalTime.of(12, 0)
      endTime isEqualTo LocalTime.of(12, 30)
      comments isEqualTo "integration test court booking comments"
    }

    // There should be 4 notifications - one owner email and 3 prisoner emails
    val notifications = notificationRepository.findAll().also { it hasSize 4 }

    notifications.isPresent("m@m.com", "new court booking prison template id with email address", persistedBooking)
    notifications.isPresent("t@t.com", "new court booking prison template id with email address", persistedBooking)
    notifications.isPresent("t@t.com", "new court booking prison template id with email address", persistedBooking)
    notifications.isPresent(TEST_USER_EMAIL, "new court booking owner template id", persistedBooking)
  }

  @Test
  fun `should create a Chesterfield court booking and emails sent to Birmingham prison`() {
    videoBookingRepository.findAll() hasSize 0
    notificationRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(birminghamLocation.key), BIRMINGHAM)
    manageUsersApi().stubGetUserDetails(TEST_USERNAME, "Test Users Name")
    manageUsersApi().stubGetUserEmail(TEST_USERNAME, TEST_USER_EMAIL)

    val courtBookingRequest = courtBookingRequest(
      courtCode = CHESTERFIELD_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = BIRMINGHAM,
      location = birminghamLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, TEST_USERNAME)

    val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    with(persistedBooking) {
      videoBookingId isEqualTo bookingId
      bookingType isEqualTo "COURT"
      court?.code isEqualTo courtBookingRequest.courtCode
      hearingType isEqualTo courtBookingRequest.courtHearingType?.name
      comments isEqualTo "integration test court booking comments"
      videoUrl isEqualTo courtBookingRequest.videoLinkUrl
      createdBy isEqualTo TEST_USERNAME
      createdByPrison isEqualTo false
    }

    with(prisonAppointmentRepository.findByVideoBooking(persistedBooking).single()) {
      videoBooking isEqualTo persistedBooking
      prisonCode isEqualTo BIRMINGHAM
      prisonerNumber isEqualTo "123456"
      appointmentType isEqualTo AppointmentType.VLB_COURT_MAIN.name
      appointmentDate isEqualTo tomorrow()
      prisonLocKey isEqualTo birminghamLocation.key
      startTime isEqualTo LocalTime.of(12, 0)
      endTime isEqualTo LocalTime.of(12, 30)
      comments isEqualTo "integration test court booking comments"
    }

    // There should be 2 - notifications one owner email and 1 prisoner email
    val notifications = notificationRepository.findAll().also { it hasSize 2 }

    notifications.isPresent("j@j.com", "new court booking prison template id no email address", persistedBooking)
    notifications.isPresent(TEST_USER_EMAIL, "new court booking owner template id", persistedBooking)
  }

  @Test
  fun `should fail to create a clashing court booking`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(birminghamLocation.key), BIRMINGHAM)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = BIRMINGHAM,
      location = birminghamLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
    )

    webTestClient.createBooking(courtBookingRequest)

    val clashingBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = BIRMINGHAM,
      location = birminghamLocation,
      startTime = LocalTime.of(12, 15),
      endTime = LocalTime.of(12, 40),
    )

    val error = webTestClient.post()
      .uri("/video-link-booking")
      .bodyValue(clashingBookingRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().is4xxClientError
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    with(error) {
      status isEqualTo 400
      userMessage isEqualTo "Exception: One or more requested court appointments overlaps with an existing appointment at location ${birminghamLocation.key}"
      developerMessage isEqualTo "One or more requested court appointments overlaps with an existing appointment at location ${birminghamLocation.key}"
    }
  }

  @Test
  fun `should fail to create a court booking when prisoner not at prison`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", MOORLAND)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(birminghamLocation.key), BIRMINGHAM)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = BIRMINGHAM,
      location = birminghamLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
    )

    val error = webTestClient.post()
      .uri("/video-link-booking")
      .bodyValue(courtBookingRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().is4xxClientError
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    with(error) {
      status isEqualTo 400
      userMessage isEqualTo "Validation failure: Prisoner 123456 not found at prison BMI"
      developerMessage isEqualTo "Prisoner 123456 not found at prison BMI"
    }
  }

  @Test
  fun `should create a probation booking`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(birminghamLocation.key), BIRMINGHAM)

    val probationBookingRequest = probationBookingRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      probationMeetingType = ProbationMeetingType.PSR,
      videoLinkUrl = "https://probation.videolink.com",
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(9, 30),
      appointmentType = AppointmentType.VLB_PROBATION,
      location = birminghamLocation,
      comments = "integration test probation booking comments",
    )

    val bookingId = webTestClient.createBooking(probationBookingRequest)

    val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    with(persistedBooking) {
      videoBookingId isEqualTo bookingId
      bookingType isEqualTo "PROBATION"
      probationTeam?.probationTeamId isEqualTo 1
      probationMeetingType isEqualTo ProbationMeetingType.PSR.name
      comments isEqualTo "integration test probation booking comments"
      videoUrl isEqualTo "https://probation.videolink.com"
      createdBy isEqualTo "booking@creator.com"
      createdByPrison isEqualTo false
    }

    with(prisonAppointmentRepository.findByVideoBooking(persistedBooking).single()) {
      videoBooking isEqualTo persistedBooking
      prisonCode isEqualTo BIRMINGHAM
      prisonerNumber isEqualTo "123456"
      appointmentType isEqualTo AppointmentType.VLB_PROBATION.name
      appointmentDate isEqualTo tomorrow()
      prisonLocKey isEqualTo birminghamLocation.key
      startTime isEqualTo LocalTime.of(9, 0)
      endTime isEqualTo LocalTime.of(9, 30)
      comments isEqualTo "integration test probation booking comments"
    }
  }

  @Test
  fun `should create two probation bookings in same location at different times`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(birminghamLocation.key), BIRMINGHAM)

    val probationBookingRequest = probationBookingRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      probationMeetingType = ProbationMeetingType.PSR,
      videoLinkUrl = "https://probation.videolink.com",
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(9, 30),
      appointmentType = AppointmentType.VLB_PROBATION,
      location = birminghamLocation,
    )

    val bookingId = webTestClient.createBooking(probationBookingRequest)

    videoBookingRepository.findById(bookingId).orElseThrow()

    prisonSearchApi().stubGetPrisoner("78910", BIRMINGHAM)

    val secondProbationBookingRequest = probationBookingRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      probationMeetingType = ProbationMeetingType.PSR,
      videoLinkUrl = "https://probation.videolink.com",
      prisonCode = BIRMINGHAM,
      prisonerNumber = "78910",
      startTime = LocalTime.of(9, 30),
      endTime = LocalTime.of(10, 30),
      appointmentType = AppointmentType.VLB_PROBATION,
      location = birminghamLocation,
    )

    val secondBookingId = webTestClient.createBooking(secondProbationBookingRequest)

    videoBookingRepository.findById(secondBookingId).orElseThrow()

    videoBookingRepository.findAll().map { it.videoBookingId } containsExactlyInAnyOrder listOf(bookingId, secondBookingId)
  }

  @Test
  fun `should fail to create a clashing probation booking`() {
    prisonSearchApi().stubGetPrisoner("123456", MOORLAND)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(moorlandLocation.key), MOORLAND)

    val probationBookingRequest = probationBookingRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      probationMeetingType = ProbationMeetingType.PSR,
      videoLinkUrl = "https://probation.videolink.com",
      prisonCode = MOORLAND,
      prisonerNumber = "123456",
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(9, 30),
      appointmentType = AppointmentType.VLB_PROBATION,
      location = moorlandLocation,
    )

    webTestClient.createBooking(probationBookingRequest)

    prisonSearchApi().stubGetPrisoner("789012", MOORLAND)

    val clashingBookingRequest = probationBookingRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      probationMeetingType = ProbationMeetingType.PSR,
      videoLinkUrl = "https://probation.videolink.com",
      prisonCode = MOORLAND,
      prisonerNumber = "789012",
      startTime = LocalTime.of(8, 0),
      endTime = LocalTime.of(9, 30),
      appointmentType = AppointmentType.VLB_PROBATION,
      location = moorlandLocation,
    )

    val error = webTestClient.post()
      .uri("/video-link-booking")
      .bodyValue(clashingBookingRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().is4xxClientError
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    with(error) {
      status isEqualTo 400
      userMessage isEqualTo "Exception: Requested probation appointment overlaps with an existing appointment at location ${moorlandLocation.key}"
      developerMessage isEqualTo "Requested probation appointment overlaps with an existing appointment at location ${moorlandLocation.key}"
    }
  }

  @Test
  fun `should fail to create a probation booking when prisoner not at prison`() {
    prisonSearchApi().stubGetPrisoner("789012", BIRMINGHAM)

    val clashingBookingRequest = probationBookingRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      probationMeetingType = ProbationMeetingType.PSR,
      videoLinkUrl = "https://probation.videolink.com",
      prisonCode = MOORLAND,
      prisonerNumber = "789012",
      startTime = LocalTime.of(8, 0),
      endTime = LocalTime.of(9, 30),
      appointmentType = AppointmentType.VLB_PROBATION,
      location = moorlandLocation,
    )

    val error = webTestClient.post()
      .uri("/video-link-booking")
      .bodyValue(clashingBookingRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().is4xxClientError
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    with(error) {
      status isEqualTo 400
      userMessage isEqualTo "Validation failure: Prisoner 789012 not found at prison MDI"
      developerMessage isEqualTo "Prisoner 789012 not found at prison MDI"
    }
  }

  @Test
  fun `should return the details of a court video link booking by ID`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", WERRINGTON)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(werringtonLocation.key), WERRINGTON)
    manageUsersApi().stubGetUserDetails(TEST_USERNAME, "Test Users Name")
    manageUsersApi().stubGetUserEmail(TEST_USERNAME, TEST_USER_EMAIL)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = WERRINGTON,
      location = werringtonLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, TEST_USERNAME)

    assertThat(bookingId).isGreaterThan(0L)

    val bookingDetails = webTestClient.getBookingByIdRequest(bookingId)

    assertThat(bookingDetails).isNotNull

    with(bookingDetails) {
      // Verify court details present for this court booking
      assertThat(courtCode).isEqualTo(DERBY_JUSTICE_CENTRE)
      assertThat(courtDescription).isEqualTo("Derby Justice Centre")
      assertThat(courtHearingType).isEqualTo(CourtHearingType.TRIBUNAL)
      assertThat(courtHearingTypeDescription).isEqualTo("Tribunal")

      // Verify probation details are null
      assertThat(probationTeamCode).isNull()
      assertThat(probationTeamDescription).isNull()
      assertThat(probationMeetingType).isNull()
      assertThat(probationMeetingTypeDescription).isNull()

      assertThat(createdByPrison).isFalse()
      assertThat(videoLinkUrl).isEqualTo("https://video.link.com")

      // Verify that there is a single appointment
      assertThat(prisonAppointments).asList().hasSize(1)
      with(prisonAppointments.first()) {
        assertThat(appointmentType).isEqualTo("VLB_COURT_MAIN")
        assertThat(comments).contains("integration test")
        assertThat(prisonCode).isEqualTo(WERRINGTON)
        assertThat(prisonLocKey).isEqualTo(werringtonLocation.key)
      }
    }
  }

  @Test
  fun `should return the details of a probation video link booking by ID`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", MOORLAND)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(moorlandLocation.key), MOORLAND)
    manageUsersApi().stubGetUserDetails(TEST_USERNAME, "Test Users Name")
    manageUsersApi().stubGetUserEmail(TEST_USERNAME, TEST_USER_EMAIL)

    val probationBookingRequest = probationBookingRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      probationMeetingType = ProbationMeetingType.PSR,
      prisonerNumber = "123456",
      prisonCode = MOORLAND,
      location = moorlandLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test probation",
    )

    val bookingId = webTestClient.createBooking(probationBookingRequest, TEST_USERNAME)

    assertThat(bookingId).isGreaterThan(0L)

    val bookingDetails = webTestClient.getBookingByIdRequest(bookingId)

    assertThat(bookingDetails).isNotNull

    with(bookingDetails) {
      // Verify probation details present
      assertThat(probationTeamCode).isEqualTo(BLACKPOOL_MC_PPOC)
      assertThat(probationTeamDescription).isEqualTo("Blackpool MC (PPOC)")
      assertThat(probationMeetingType).isEqualTo(ProbationMeetingType.PSR)
      assertThat(probationMeetingTypeDescription).isEqualTo("Pre-sentence report")

      // Verify court details are null
      assertThat(courtCode).isNull()
      assertThat(courtDescription).isNull()
      assertThat(courtHearingType).isNull()
      assertThat(courtHearingTypeDescription).isNull()

      assertThat(createdByPrison).isFalse()
      assertThat(videoLinkUrl).isEqualTo("https://video.link.com")

      // Verify that there is a single appointment
      assertThat(prisonAppointments).asList().hasSize(1)
      with(prisonAppointments.first()) {
        assertThat(appointmentType).isEqualTo("VLB_PROBATION")
        assertThat(comments).contains("integration test")
        assertThat(prisonCode).isEqualTo(MOORLAND)
        assertThat(prisonLocKey).isEqualTo(moorlandLocation.key)
      }
    }
  }

  @Test
  fun `should return a 404 not found when requesting an invalid video booking ID`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", WERRINGTON)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(werringtonLocation.key), WERRINGTON)
    manageUsersApi().stubGetUserDetails(TEST_USERNAME, "Test Users Name")
    manageUsersApi().stubGetUserEmail(TEST_USERNAME, TEST_USER_EMAIL)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = WERRINGTON,
      location = werringtonLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test not found",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, TEST_USERNAME)

    assertThat(bookingId).isGreaterThan(0L)

    val errorResponse = webTestClient.getBookingByIdNotFound(bookingId + 300)

    assertThat(errorResponse).isNotNull
    assertThat(errorResponse.status).isEqualTo(404)
  }

  @Test
  fun `should amend a Derby court booking and emails sent to Werrington prison`() {
    videoBookingRepository.findAll() hasSize 0
    notificationRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", WERRINGTON)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(werringtonLocation.key), WERRINGTON)
    manageUsersApi().stubGetUserDetails(TEST_USERNAME, "Test Users Name")
    manageUsersApi().stubGetUserEmail(TEST_USERNAME, TEST_USER_EMAIL)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = WERRINGTON,
      location = werringtonLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest)

    val amendBookingRequest = amendCourtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = WERRINGTON,
      location = werringtonLocation,
      startTime = LocalTime.of(13, 0),
      endTime = LocalTime.of(14, 30),
      comments = "amended court booking comments",
    )

    notificationRepository.deleteAll()
    webTestClient.amendBooking(bookingId, amendBookingRequest, TEST_USERNAME)

    val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    with(persistedBooking) {
      videoBookingId isEqualTo bookingId
      bookingType isEqualTo "COURT"
      court?.code isEqualTo courtBookingRequest.courtCode
      hearingType isEqualTo courtBookingRequest.courtHearingType?.name
      comments isEqualTo "amended court booking comments"
      videoUrl isEqualTo courtBookingRequest.videoLinkUrl
      createdBy isEqualTo "booking@creator.com"
      createdByPrison isEqualTo false
      amendedBy isEqualTo TEST_USERNAME
    }

    with(prisonAppointmentRepository.findByVideoBooking(persistedBooking).single()) {
      videoBooking isEqualTo persistedBooking
      prisonCode isEqualTo WERRINGTON
      prisonerNumber isEqualTo "123456"
      appointmentType isEqualTo AppointmentType.VLB_COURT_MAIN.name
      appointmentDate isEqualTo tomorrow()
      prisonLocKey isEqualTo werringtonLocation.key
      startTime isEqualTo LocalTime.of(13, 0)
      endTime isEqualTo LocalTime.of(14, 30)
      comments isEqualTo "amended court booking comments"
    }

    // There should be 4 notifications one owner email and 3 prisoner emails
    val notifications = notificationRepository.findAll().also { it hasSize 4 }

    notifications.isPresent("m@m.com", "amended court booking prison template id with email address", persistedBooking)
    notifications.isPresent("t@t.com", "amended court booking prison template id with email address", persistedBooking)
    notifications.isPresent("t@t.com", "amended court booking prison template id with email address", persistedBooking)
    notifications.isPresent(TEST_USER_EMAIL, "amended court booking owner template id", persistedBooking)
  }

  @Test
  fun `should amend a Chesterfield court booking and emails sent to Birmingham prison`() {
    videoBookingRepository.findAll() hasSize 0
    notificationRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(birminghamLocation.key), BIRMINGHAM)
    manageUsersApi().stubGetUserDetails(TEST_USERNAME, "Test Users Name")
    manageUsersApi().stubGetUserEmail(TEST_USERNAME, TEST_USER_EMAIL)

    val courtBookingRequest = courtBookingRequest(
      courtCode = CHESTERFIELD_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = BIRMINGHAM,
      location = birminghamLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest)

    val amendBookingRequest = amendCourtBookingRequest(
      courtCode = CHESTERFIELD_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = BIRMINGHAM,
      location = birminghamLocation,
      startTime = LocalTime.of(13, 0),
      endTime = LocalTime.of(14, 30),
      comments = "amended court booking comments",
    )

    notificationRepository.deleteAll()
    webTestClient.amendBooking(bookingId, amendBookingRequest, TEST_USERNAME)

    val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    with(persistedBooking) {
      videoBookingId isEqualTo bookingId
      bookingType isEqualTo "COURT"
      court?.code isEqualTo courtBookingRequest.courtCode
      hearingType isEqualTo courtBookingRequest.courtHearingType?.name
      comments isEqualTo "amended court booking comments"
      videoUrl isEqualTo courtBookingRequest.videoLinkUrl
      createdBy isEqualTo "booking@creator.com"
      createdByPrison isEqualTo false
      amendedBy isEqualTo TEST_USERNAME
    }

    with(prisonAppointmentRepository.findByVideoBooking(persistedBooking).single()) {
      videoBooking isEqualTo persistedBooking
      prisonCode isEqualTo BIRMINGHAM
      prisonerNumber isEqualTo "123456"
      appointmentType isEqualTo AppointmentType.VLB_COURT_MAIN.name
      appointmentDate isEqualTo tomorrow()
      prisonLocKey isEqualTo birminghamLocation.key
      startTime isEqualTo LocalTime.of(13, 0)
      endTime isEqualTo LocalTime.of(14, 30)
      comments isEqualTo "amended court booking comments"
    }

    // There should be 2 notifications - one owner email and 1 prisoner email
    val notifications = notificationRepository.findAll().also { it hasSize 2 }

    notifications.isPresent("j@j.com", "amended court booking prison template id no email address", persistedBooking)
    notifications.isPresent(TEST_USER_EMAIL, "amended court booking owner template id", persistedBooking)
  }

  @Test
  fun `should fail to amend to a clashing court booking`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(birminghamLocation.key), BIRMINGHAM)

    val courtBookingRequest1 = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = BIRMINGHAM,
      location = birminghamLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
    )
    val courtBookingRequest2 = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = BIRMINGHAM,
      location = birminghamLocation,
      startTime = LocalTime.of(15, 0),
      endTime = LocalTime.of(16, 30),
    )

    webTestClient.createBooking(courtBookingRequest1)
    val videoBookingId = webTestClient.createBooking(courtBookingRequest2)

    val clashingBookingRequest = amendCourtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = BIRMINGHAM,
      location = birminghamLocation,
      startTime = LocalTime.of(12, 15),
      endTime = LocalTime.of(12, 40),
    )

    val error = webTestClient.put()
      .uri("/video-link-booking/id/$videoBookingId")
      .bodyValue(clashingBookingRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().is4xxClientError
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    with(error) {
      status isEqualTo 400
      userMessage isEqualTo "Exception: One or more requested court appointments overlaps with an existing appointment at location ${birminghamLocation.key}"
      developerMessage isEqualTo "One or more requested court appointments overlaps with an existing appointment at location ${birminghamLocation.key}"
    }
  }

  @Test
  fun `should fail to amend a court booking when prisoner not at prison`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(birminghamLocation.key), BIRMINGHAM)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = BIRMINGHAM,
      location = birminghamLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
    )

    val videoBookingId = webTestClient.createBooking(courtBookingRequest)

    prisonSearchApi().stubGetPrisoner("123456", MOORLAND)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(birminghamLocation.key), BIRMINGHAM)

    val amendBookingRequest = amendCourtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = BIRMINGHAM,
      location = birminghamLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
    )

    val error = webTestClient.put()
      .uri("/video-link-booking/id/$videoBookingId")
      .bodyValue(amendBookingRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().is4xxClientError
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    with(error) {
      status isEqualTo 400
      userMessage isEqualTo "Validation failure: Prisoner 123456 not found at prison BMI"
      developerMessage isEqualTo "Prisoner 123456 not found at prison BMI"
    }
  }

  @Test
  fun `should amend a probation booking`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(birminghamLocation.key), BIRMINGHAM)

    val probationBookingRequest = probationBookingRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      probationMeetingType = ProbationMeetingType.PSR,
      videoLinkUrl = "https://probation.videolink.com",
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(9, 30),
      appointmentType = AppointmentType.VLB_PROBATION,
      location = birminghamLocation,
      comments = "integration test probation booking comments",
    )

    val bookingId = webTestClient.createBooking(probationBookingRequest)

    val amendBookingRequest = amendProbationBookingRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      probationMeetingType = ProbationMeetingType.PSR,
      videoLinkUrl = "https://probation.videolink.com",
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      startTime = LocalTime.of(10, 0),
      endTime = LocalTime.of(11, 30),
      appointmentType = AppointmentType.VLB_PROBATION,
      location = birminghamLocation,
      comments = "amended probation booking comments",
    )

    webTestClient.amendBooking(bookingId, amendBookingRequest, TEST_USERNAME)

    val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    with(persistedBooking) {
      videoBookingId isEqualTo bookingId
      bookingType isEqualTo "PROBATION"
      probationTeam?.probationTeamId isEqualTo 1
      probationMeetingType isEqualTo ProbationMeetingType.PSR.name
      comments isEqualTo "amended probation booking comments"
      videoUrl isEqualTo "https://probation.videolink.com"
      createdBy isEqualTo "booking@creator.com"
      createdByPrison isEqualTo false
      amendedBy isEqualTo TEST_USERNAME
    }

    with(prisonAppointmentRepository.findByVideoBooking(persistedBooking).single()) {
      videoBooking isEqualTo persistedBooking
      prisonCode isEqualTo BIRMINGHAM
      prisonerNumber isEqualTo "123456"
      appointmentType isEqualTo AppointmentType.VLB_PROBATION.name
      appointmentDate isEqualTo tomorrow()
      prisonLocKey isEqualTo birminghamLocation.key
      startTime isEqualTo LocalTime.of(10, 0)
      endTime isEqualTo LocalTime.of(11, 30)
      comments isEqualTo "amended probation booking comments"
    }
  }

  @Test
  fun `should fail to amend to a clashing probation booking`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(birminghamLocation.key), BIRMINGHAM)

    val probationBookingRequest1 = probationBookingRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      probationMeetingType = ProbationMeetingType.PSR,
      videoLinkUrl = "https://probation.videolink.com",
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(9, 30),
      appointmentType = AppointmentType.VLB_PROBATION,
      location = birminghamLocation,
      comments = "integration test probation booking comments",
    )
    val probationBookingRequest2 = probationBookingRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      probationMeetingType = ProbationMeetingType.PSR,
      videoLinkUrl = "https://probation.videolink.com",
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      startTime = LocalTime.of(15, 0),
      endTime = LocalTime.of(16, 30),
      appointmentType = AppointmentType.VLB_PROBATION,
      location = birminghamLocation,
      comments = "integration test probation booking comments",
    )

    webTestClient.createBooking(probationBookingRequest1)
    val videoBookingId = webTestClient.createBooking(probationBookingRequest2)

    val clashingBookingRequest = amendProbationBookingRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      probationMeetingType = ProbationMeetingType.PSR,
      videoLinkUrl = "https://probation.videolink.com",
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(9, 30),
      appointmentType = AppointmentType.VLB_PROBATION,
      location = birminghamLocation,
      comments = "integration test probation booking comments",
    )

    val error = webTestClient.put()
      .uri("/video-link-booking/id/$videoBookingId")
      .bodyValue(clashingBookingRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().is4xxClientError
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    with(error) {
      status isEqualTo 400
      userMessage isEqualTo "Exception: Requested probation appointment overlaps with an existing appointment at location ${birminghamLocation.key}"
      developerMessage isEqualTo "Requested probation appointment overlaps with an existing appointment at location ${birminghamLocation.key}"
    }
  }

  @Test
  fun `should fail to amend a probation booking when prisoner not at prison`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(birminghamLocation.key), BIRMINGHAM)

    val probationBookingRequest = probationBookingRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      probationMeetingType = ProbationMeetingType.PSR,
      videoLinkUrl = "https://probation.videolink.com",
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(9, 30),
      appointmentType = AppointmentType.VLB_PROBATION,
      location = birminghamLocation,
      comments = "integration test probation booking comments",
    )

    val videoBookingId = webTestClient.createBooking(probationBookingRequest)

    prisonSearchApi().stubGetPrisoner("123456", MOORLAND)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(birminghamLocation.key), BIRMINGHAM)

    val amendBookingRequest = amendProbationBookingRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      probationMeetingType = ProbationMeetingType.PSR,
      videoLinkUrl = "https://probation.videolink.com",
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(9, 30),
      appointmentType = AppointmentType.VLB_PROBATION,
      location = birminghamLocation,
      comments = "integration test probation booking comments",
    )

    val error = webTestClient.put()
      .uri("/video-link-booking/id/$videoBookingId")
      .bodyValue(amendBookingRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().is4xxClientError
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    with(error) {
      status isEqualTo 400
      userMessage isEqualTo "Validation failure: Prisoner 123456 not found at prison BMI"
      developerMessage isEqualTo "Prisoner 123456 not found at prison BMI"
    }
  }

  @Test
  fun `should return a 404 not found when amending an invalid video booking ID`() {
    videoBookingRepository.findAll() hasSize 0

    val errorResponse = webTestClient.getBookingByIdNotFound(1L)

    assertThat(errorResponse).isNotNull
    assertThat(errorResponse.status).isEqualTo(404)
  }

  @Test
  fun `should cancel a Chesterfield court booking`() {
    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(birminghamLocation.key), BIRMINGHAM)
    manageUsersApi().stubGetUserDetails(TEST_USERNAME, "Test Users Name")
    manageUsersApi().stubGetUserEmail(TEST_USERNAME, TEST_USER_EMAIL)

    val courtBookingRequest = courtBookingRequest(
      courtCode = CHESTERFIELD_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = BIRMINGHAM,
      location = birminghamLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest)

    val activeBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    activeBooking.statusCode isEqualTo StatusCode.ACTIVE

    webTestClient.cancelBooking(bookingId)

    val cancelledBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    cancelledBooking.statusCode isEqualTo StatusCode.CANCELLED
  }

  private fun WebTestClient.createBooking(request: CreateVideoBookingRequest, username: String = "booking@creator.com") =
    this
      .post()
      .uri("/video-link-booking")
      .bodyValue(request)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Long::class.java)
      .returnResult().responseBody!!

  private fun WebTestClient.amendBooking(videoBookingId: Long, request: AmendVideoBookingRequest, username: String = "booking@creator.com") =
    this
      .put()
      .uri("/video-link-booking/id/$videoBookingId")
      .bodyValue(request)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Long::class.java)
      .returnResult().responseBody!!

  private fun WebTestClient.cancelBooking(videoBookingId: Long, username: String = "booking@cancel.com") =
    this
      .delete()
      .uri("/video-link-booking/id/$videoBookingId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isNoContent

  private fun WebTestClient.getBookingByIdRequest(videoBookingId: Long, username: String = "booking@creator.com") =
    this
      .get()
      .uri("/video-link-booking/id/{videoBookinId}", videoBookingId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(VideoLinkBooking::class.java)
      .returnResult().responseBody!!

  private fun WebTestClient.getBookingByIdNotFound(videoBookingId: Long, username: String = "booking@creator.com") =
    this
      .get()
      .uri("/video-link-booking/id/{videoBookinId}", videoBookingId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

  private fun Collection<Notification>.isPresent(email: String, template: String, booking: VideoBooking) {
    with(single { it.email == email }) {
      templateName isEqualTo template
      videoBooking isEqualTo booking
    }
  }
}

@TestConfiguration
class TestEmailConfiguration {
  @Bean
  fun emailService() =
    EmailService { email ->
      when (email) {
        is NewCourtBookingEmail -> Result.success(UUID.randomUUID() to "new court booking owner template id")
        is NewCourtBookingPrisonCourtEmail -> Result.success(UUID.randomUUID() to "new court booking prison template id with email address")
        is NewCourtBookingPrisonNoCourtEmail -> Result.success(UUID.randomUUID() to "new court booking prison template id no email address")
        is AmendedCourtBookingEmail -> Result.success(UUID.randomUUID() to "amended court booking owner template id")
        is AmendedCourtBookingPrisonCourtEmail -> Result.success(UUID.randomUUID() to "amended court booking prison template id with email address")
        is AmendedCourtBookingPrisonNoCourtEmail -> Result.success(UUID.randomUUID() to "amended court booking prison template id no email address")
        is CancelledCourtBookingEmail -> Result.success(UUID.randomUUID() to "cancelled court booking owner template id")
        is CancelledCourtBookingPrisonCourtEmail -> Result.success(UUID.randomUUID() to "cancelled court booking prison template id with email address")
        is CancelledCourtBookingPrisonNoCourtEmail -> Result.success(UUID.randomUUID() to "cancelled court booking prison template id no email address")
        else -> throw RuntimeException("Unsupported email")
      }
    }
}

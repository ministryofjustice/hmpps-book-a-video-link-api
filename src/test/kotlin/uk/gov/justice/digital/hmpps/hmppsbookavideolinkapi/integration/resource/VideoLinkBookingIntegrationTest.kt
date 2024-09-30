package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Notification
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.StatusCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BLACKPOOL_MC_PPOC
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.CHESTERFIELD_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.HARROW
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.NORWICH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WERRINGTON
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendCourtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendProbationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isInstanceOf
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isNotEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.norwichLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.requestCourtVideoLinkRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.requestProbationVideoLinkRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.werringtonLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.RequestVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.VideoBookingSearchRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.VideoLinkBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User
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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.AppointmentCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.AppointmentInformation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.ManageExternalAppointmentsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.VideoBookingCancelledEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.VideoBookingCreatedEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.VideoBookingInformation
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@ContextConfiguration(classes = [TestEmailConfiguration::class])
// This is not ideal. Due to potential timing issues with messages/events we disable this on the CI pipeline.
// We need a better solution to this.
@DisabledIfEnvironmentVariable(named = "CIRCLECI", matches = "true")
class VideoLinkBookingIntegrationTest : SqsIntegrationTestBase() {

  @SpyBean
  private lateinit var outboundEventsPublisher: OutboundEventsPublisher

  @MockBean
  private lateinit var manageExternalAppointmentsService: ManageExternalAppointmentsService

  @Autowired
  private lateinit var videoBookingRepository: VideoBookingRepository

  @Autowired
  private lateinit var prisonAppointmentRepository: PrisonAppointmentRepository

  @Autowired
  private lateinit var notificationRepository: NotificationRepository

  @Autowired
  private lateinit var bookingHistoryRepository: BookingHistoryRepository

  @Test
  fun `should create a Derby court booking as court users and emails sent to Werrington prison`() {
    videoBookingRepository.findAll() hasSize 0
    notificationRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", WERRINGTON)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(werringtonLocation.key), WERRINGTON)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = WERRINGTON,
      location = werringtonLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, COURT_USER)

    val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    with(persistedBooking) {
      videoBookingId isEqualTo bookingId
      bookingType isEqualTo "COURT"
      court?.code isEqualTo courtBookingRequest.courtCode
      hearingType isEqualTo courtBookingRequest.courtHearingType?.name
      comments isEqualTo "integration test court booking comments"
      videoUrl isEqualTo courtBookingRequest.videoLinkUrl
      createdBy isEqualTo COURT_USER.username
      createdByPrison isEqualTo false
    }

    val persistedAppointment = prisonAppointmentRepository.findByVideoBooking(persistedBooking).single()

    with(persistedAppointment) {
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

    val history = bookingHistoryRepository.findAllByVideoBookingIdOrderByCreatedTime(persistedBooking.videoBookingId)
    with(history.first()) {
      historyType isEqualTo HistoryType.CREATE
      videoBookingId isEqualTo persistedBooking.videoBookingId
      hearingType isEqualTo persistedBooking.hearingType
      courtId isEqualTo persistedBooking.court?.courtId
      appointments() hasSize 1
    }

    // There should be 2 notifications - 1 user email and 1 prison email
    val notifications = notificationRepository.findAll().also { it hasSize 2 }

    notifications.isPresent("t@t.com", "new court booking prison template id with email address", persistedBooking)
    notifications.isPresent(COURT_USER.email!!, "new court booking user template id", persistedBooking)

    2.messagesShouldBePublished {
      firstValue isInstanceOf VideoBookingCreatedEvent::class.java
      firstValue.additionalInformation isEqualTo VideoBookingInformation(persistedBooking.videoBookingId)

      secondValue isInstanceOf AppointmentCreatedEvent::class.java
      secondValue.additionalInformation isEqualTo AppointmentInformation(persistedAppointment.prisonAppointmentId)
    }
  }

  @Test
  fun `should create a Derby court booking as prison user and emails sent to Werrington prison and Derby court`() {
    val prisonUser = PRISON_USER.copy(activeCaseLoadId = WERRINGTON).also(::stubUser)

    videoBookingRepository.findAll() hasSize 0
    notificationRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", WERRINGTON)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(werringtonLocation.key), WERRINGTON)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = WERRINGTON,
      location = werringtonLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, prisonUser)

    val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    with(persistedBooking) {
      videoBookingId isEqualTo bookingId
      bookingType isEqualTo "COURT"
      court?.code isEqualTo courtBookingRequest.courtCode
      hearingType isEqualTo courtBookingRequest.courtHearingType?.name
      comments isEqualTo "integration test court booking comments"
      videoUrl isEqualTo courtBookingRequest.videoLinkUrl
      createdBy isEqualTo PRISON_USER.username
      createdByPrison isEqualTo true
    }

    val persistedAppointment = prisonAppointmentRepository.findByVideoBooking(persistedBooking).single()

    with(persistedAppointment) {
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

    val history = bookingHistoryRepository.findAllByVideoBookingIdOrderByCreatedTime(persistedBooking.videoBookingId)
    with(history.first()) {
      historyType isEqualTo HistoryType.CREATE
      videoBookingId isEqualTo persistedBooking.videoBookingId
      hearingType isEqualTo persistedBooking.hearingType
      courtId isEqualTo persistedBooking.court?.courtId
      appointments() hasSize 1
    }

    // There should be 3 notifications - 1 user email, 1 court email and 1 prison email
    val notifications = notificationRepository.findAll().also { it hasSize 3 }

    notifications.isPresent("t@t.com", "new court booking prison template id with email address", persistedBooking)
    notifications.isPresent("j@j.com", "new court booking court template id", persistedBooking)
    notifications.isPresent(prisonUser.email!!, "new court booking user template id", persistedBooking)

    2.messagesShouldBePublished {
      firstValue isInstanceOf VideoBookingCreatedEvent::class.java
      firstValue.additionalInformation isEqualTo VideoBookingInformation(persistedBooking.videoBookingId)

      secondValue isInstanceOf AppointmentCreatedEvent::class.java
      secondValue.additionalInformation isEqualTo AppointmentInformation(persistedAppointment.prisonAppointmentId)
    }
  }

  @Test
  fun `should allow creation of overlapping court bookings as a prison user`() {
    val prisonUser = PRISON_USER.copy(activeCaseLoadId = WERRINGTON).also(::stubUser)

    videoBookingRepository.findAll() hasSize 0
    notificationRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", WERRINGTON)
    prisonSearchApi().stubGetPrisoner("789101", WERRINGTON)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(werringtonLocation.key), WERRINGTON)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = WERRINGTON,
      location = werringtonLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    webTestClient.createBooking(courtBookingRequest, prisonUser)

    val overlappingCourtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "789101",
      prisonCode = WERRINGTON,
      location = werringtonLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    // No error should be thrown
    webTestClient.createBooking(overlappingCourtBookingRequest, prisonUser)
  }

  @Test
  fun `should reject duplicate court booking creation as a prison user`() {
    val prisonUser = PRISON_USER.copy(activeCaseLoadId = WERRINGTON).also(::stubUser)

    videoBookingRepository.findAll() hasSize 0
    notificationRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", WERRINGTON)
    prisonSearchApi().stubGetPrisoner("789101", WERRINGTON)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(werringtonLocation.key), WERRINGTON)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = WERRINGTON,
      location = werringtonLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    webTestClient.createBooking(courtBookingRequest, prisonUser)
    webTestClient.createBookingFails(courtBookingRequest, prisonUser).expectStatus().isBadRequest
  }

  @Test
  fun `should create a Chesterfield court booking as court user and emails sent to Birmingham prison`() {
    videoBookingRepository.findAll() hasSize 0
    notificationRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(birminghamLocation.key), BIRMINGHAM)

    val courtBookingRequest = courtBookingRequest(
      courtCode = CHESTERFIELD_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = BIRMINGHAM,
      location = birminghamLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, COURT_USER)

    val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    with(persistedBooking) {
      videoBookingId isEqualTo bookingId
      bookingType isEqualTo "COURT"
      court?.code isEqualTo courtBookingRequest.courtCode
      hearingType isEqualTo courtBookingRequest.courtHearingType?.name
      comments isEqualTo "integration test court booking comments"
      videoUrl isEqualTo courtBookingRequest.videoLinkUrl
      createdBy isEqualTo COURT_USER.username
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

    // There should be 2 - notifications one user email and 1 prisoner email
    val notifications = notificationRepository.findAll().also { it hasSize 2 }

    notifications.isPresent("j@j.com", "new court booking prison template id no email address", persistedBooking)
    notifications.isPresent(COURT_USER.email!!, "new court booking user template id", persistedBooking)
  }

  @Test
  fun `should fail to create a clashing court booking as court user`() {
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

    webTestClient.createBooking(courtBookingRequest, COURT_USER)

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
      .headers(setAuthorisation(user = COURT_USER.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
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
  fun `should create a court booking which clashes with a future cancelled booking as court user`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(birminghamLocation.key), BIRMINGHAM)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = BIRMINGHAM,
      location = birminghamLocation,
      date = tomorrow(),
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
    )

    val originalBooking = webTestClient.createBooking(courtBookingRequest, COURT_USER)

    webTestClient.cancelBooking(originalBooking, COURT_USER)

    videoBookingRepository.findById(originalBooking).orElseThrow().statusCode isEqualTo StatusCode.CANCELLED

    val clashingBooking = webTestClient.createBooking(courtBookingRequest, COURT_USER)

    originalBooking isNotEqualTo clashingBooking

    videoBookingRepository.findById(clashingBooking).orElseThrow().statusCode isEqualTo StatusCode.ACTIVE
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
      .headers(setAuthorisation(user = COURT_USER.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
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
  fun `should create a probation booking as probation user`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)
    locationsInsidePrisonApi().stubGetLocationByKey(birminghamLocation.key, BIRMINGHAM)

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

    val bookingId = webTestClient.createBooking(probationBookingRequest, PROBATION_USER)

    val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    with(persistedBooking) {
      videoBookingId isEqualTo bookingId
      bookingType isEqualTo "PROBATION"
      probationTeam?.probationTeamId isEqualTo 1
      probationMeetingType isEqualTo ProbationMeetingType.PSR.name
      comments isEqualTo "integration test probation booking comments"
      videoUrl isEqualTo "https://probation.videolink.com"
      createdBy isEqualTo PROBATION_USER.username
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

    val history = bookingHistoryRepository.findAllByVideoBookingIdOrderByCreatedTime(persistedBooking.videoBookingId)
    with(history.first()) {
      historyType isEqualTo HistoryType.CREATE
      videoBookingId isEqualTo persistedBooking.videoBookingId
      probationMeetingType isEqualTo persistedBooking.probationMeetingType
      probationTeamId isEqualTo persistedBooking.probationTeam?.probationTeamId
      appointments() hasSize 1
    }
  }

  @Test
  fun `should create two probation bookings in same location at different times as probation user`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)
    locationsInsidePrisonApi().stubGetLocationByKey(birminghamLocation.key, BIRMINGHAM)

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

    val bookingId = webTestClient.createBooking(probationBookingRequest, PROBATION_USER)

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

    val secondBookingId = webTestClient.createBooking(secondProbationBookingRequest, PROBATION_USER)

    videoBookingRepository.findById(secondBookingId).orElseThrow()

    videoBookingRepository.findAll().map { it.videoBookingId } containsExactlyInAnyOrder listOf(bookingId, secondBookingId)
  }

  @Test
  fun `should fail to create a clashing probation booking as probation user`() {
    prisonSearchApi().stubGetPrisoner("123456", MOORLAND)
    locationsInsidePrisonApi().stubGetLocationByKey(moorlandLocation.key, MOORLAND)

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

    webTestClient.createBooking(probationBookingRequest, PROBATION_USER)

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
      .headers(setAuthorisation(user = PROBATION_USER.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
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
      .headers(setAuthorisation(user = PROBATION_USER.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
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
  fun `should fail to create a probation booking when user is prison user`() {
    val error = webTestClient.createBookingFails(probationBookingRequest(), PRISON_USER)
      .expectStatus().isBadRequest
      .expectStatus().is4xxClientError
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    with(error) {
      status isEqualTo 400
      userMessage isEqualTo "Exception: Only probation users can create probation bookings."
      developerMessage isEqualTo "Only probation users can create probation bookings."
    }
  }

  @Test
  fun `should return the details of a court video link booking by ID`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", WERRINGTON)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(werringtonLocation.key), WERRINGTON)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = WERRINGTON,
      location = werringtonLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, COURT_USER)

    assertThat(bookingId).isGreaterThan(0L)

    val bookingDetails = webTestClient.getBookingByIdRequest(bookingId, COURT_USER)

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
      assertThat(prisonAppointments).hasSize(1)
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
    locationsInsidePrisonApi().stubGetLocationByKey(moorlandLocation.key, MOORLAND)

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

    val bookingId = webTestClient.createBooking(probationBookingRequest, PROBATION_USER)

    assertThat(bookingId).isGreaterThan(0L)

    val bookingDetails = webTestClient.getBookingByIdRequest(bookingId, PROBATION_USER)

    assertThat(bookingDetails).isNotNull

    with(bookingDetails) {
      // Verify probation details present
      assertThat(probationTeamCode).isEqualTo(BLACKPOOL_MC_PPOC)
      assertThat(probationTeamDescription).isEqualTo("Blackpool MC - Probation")
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
      assertThat(prisonAppointments).hasSize(1)
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

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = WERRINGTON,
      location = werringtonLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test not found",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, COURT_USER)

    assertThat(bookingId).isGreaterThan(0L)

    val errorResponse = webTestClient.getBookingByIdNotFound(bookingId + 300, COURT_USER)

    assertThat(errorResponse).isNotNull
    assertThat(errorResponse.status).isEqualTo(404)
  }

  @Test
  fun `should amend a Derby court booking and emails sent to Werrington prison`() {
    videoBookingRepository.findAll() hasSize 0
    notificationRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", WERRINGTON)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(werringtonLocation.key), WERRINGTON)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = WERRINGTON,
      location = werringtonLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, COURT_USER)

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
    webTestClient.amendBooking(bookingId, amendBookingRequest, COURT_USER)

    val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    with(persistedBooking) {
      videoBookingId isEqualTo bookingId
      bookingType isEqualTo "COURT"
      court?.code isEqualTo courtBookingRequest.courtCode
      hearingType isEqualTo courtBookingRequest.courtHearingType?.name
      comments isEqualTo "amended court booking comments"
      videoUrl isEqualTo courtBookingRequest.videoLinkUrl
      createdBy isEqualTo COURT_USER.username
      createdByPrison isEqualTo false
      amendedBy isEqualTo COURT_USER.username
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

    // There should be 2 notifications, 1 user email and 1 prison email
    val notifications = notificationRepository.findAll().also { it hasSize 2 }

    notifications.isPresent("t@t.com", "amended court booking prison template id with email address", persistedBooking)
    notifications.isPresent(COURT_USER.email!!, "amended court booking user template id", persistedBooking)
  }

  @Test
  fun `should amend a Chesterfield court booking and emails sent to Birmingham prison`() {
    videoBookingRepository.findAll() hasSize 0
    notificationRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(birminghamLocation.key), BIRMINGHAM)

    val courtBookingRequest = courtBookingRequest(
      courtCode = CHESTERFIELD_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = BIRMINGHAM,
      location = birminghamLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, COURT_USER)

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
    webTestClient.amendBooking(bookingId, amendBookingRequest, COURT_USER)

    val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    with(persistedBooking) {
      videoBookingId isEqualTo bookingId
      bookingType isEqualTo "COURT"
      court?.code isEqualTo courtBookingRequest.courtCode
      hearingType isEqualTo courtBookingRequest.courtHearingType?.name
      comments isEqualTo "amended court booking comments"
      videoUrl isEqualTo courtBookingRequest.videoLinkUrl
      createdBy isEqualTo COURT_USER.username
      createdByPrison isEqualTo false
      amendedBy isEqualTo COURT_USER.username
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

    // There should be 2 notifications - one user email and 1 prisoner email
    val notifications = notificationRepository.findAll().also { it hasSize 2 }

    notifications.isPresent("j@j.com", "amended court booking prison template id no email address", persistedBooking)
    notifications.isPresent(COURT_USER.email!!, "amended court booking user template id", persistedBooking)
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

    webTestClient.createBooking(courtBookingRequest1, COURT_USER)
    val videoBookingId = webTestClient.createBooking(courtBookingRequest2, COURT_USER)

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
      .headers(setAuthorisation(user = COURT_USER.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
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

    val videoBookingId = webTestClient.createBooking(courtBookingRequest, COURT_USER)

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
      .headers(setAuthorisation(user = COURT_USER.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
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
    locationsInsidePrisonApi().stubGetLocationByKey(birminghamLocation.key, BIRMINGHAM)

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

    val bookingId = webTestClient.createBooking(probationBookingRequest, PROBATION_USER)

    val amendBookingRequest = amendProbationBookingRequest(
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

    webTestClient.amendBooking(bookingId, amendBookingRequest, PROBATION_USER)

    val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    with(persistedBooking) {
      videoBookingId isEqualTo bookingId
      bookingType isEqualTo "PROBATION"
      probationTeam?.probationTeamId isEqualTo 1
      probationMeetingType isEqualTo ProbationMeetingType.PSR.name
      comments isEqualTo "amended probation booking comments"
      videoUrl isEqualTo "https://probation.videolink.com"
      createdBy isEqualTo PROBATION_USER.username
      createdByPrison isEqualTo false
      amendedBy isEqualTo PROBATION_USER.username
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
    locationsInsidePrisonApi().stubGetLocationByKey(birminghamLocation.key, BIRMINGHAM)

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

    webTestClient.createBooking(probationBookingRequest1, PROBATION_USER)
    val videoBookingId = webTestClient.createBooking(probationBookingRequest2, PROBATION_USER)

    val clashingBookingRequest = amendProbationBookingRequest(
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
      .headers(setAuthorisation(user = PROBATION_USER.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
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
    locationsInsidePrisonApi().stubGetLocationByKey(birminghamLocation.key, BIRMINGHAM)

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

    val videoBookingId = webTestClient.createBooking(probationBookingRequest, PROBATION_USER)

    prisonSearchApi().stubGetPrisoner("123456", MOORLAND)
    locationsInsidePrisonApi().stubGetLocationByKey(birminghamLocation.key, BIRMINGHAM)

    val amendBookingRequest = amendProbationBookingRequest(
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
      .headers(setAuthorisation(user = PROBATION_USER.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
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

    val errorResponse = webTestClient.getBookingByIdNotFound(1L, COURT_USER)

    assertThat(errorResponse).isNotNull
    assertThat(errorResponse.status).isEqualTo(404)
  }

  @Test
  fun `should cancel a Chesterfield court booking`() {
    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)
    prisonerApi().stubGetInternalLocationByKey(birminghamLocation.key, BIRMINGHAM)
    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(birminghamLocation.key), BIRMINGHAM)

    val courtBookingRequest = courtBookingRequest(
      courtCode = CHESTERFIELD_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = BIRMINGHAM,
      location = birminghamLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, COURT_USER)

    val activeBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    activeBooking.statusCode isEqualTo StatusCode.ACTIVE

    webTestClient.cancelBooking(bookingId, COURT_USER)

    3.messagesShouldBePublished {
      firstValue isInstanceOf VideoBookingCreatedEvent::class.java
      secondValue isInstanceOf AppointmentCreatedEvent::class.java
      thirdValue isInstanceOf VideoBookingCancelledEvent::class.java
    }

    val cancelledBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    cancelledBooking.statusCode isEqualTo StatusCode.CANCELLED
  }

  @Test
  fun `should request a Derby court booking and emails sent to Norwich prison`() {
    notificationRepository.findAll() hasSize 0

    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(norwichLocation.key), NORWICH)

    val courtRequest = requestCourtVideoLinkRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      firstName = "John",
      lastName = "Smith",
      dateOfBirth = LocalDate.of(1970, 1, 1),
      prisonCode = NORWICH,
      location = norwichLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court request comments",
    )

    webTestClient.requestVideoLink(courtRequest)

    // There should be 2 notifications - 1 user email and 1 prison email
    val notifications = notificationRepository.findAll().also { it hasSize 2 }

    notifications.isPresent("r@r.com", "requested court booking prison template id with email address")
    notifications.isPresent(PRISON_USER.email!!, "requested court booking user template id")
  }

  @Test
  fun `should request a Chesterfield court booking and emails sent to Birmingham prison`() {
    notificationRepository.findAll() hasSize 0

    locationsInsidePrisonApi().stubPostLocationByKeys(setOf(birminghamLocation.key), BIRMINGHAM)

    val courtRequest = requestCourtVideoLinkRequest(
      courtCode = CHESTERFIELD_JUSTICE_CENTRE,
      firstName = "John",
      lastName = "Smith",
      dateOfBirth = LocalDate.of(1970, 1, 1),
      prisonCode = BIRMINGHAM,
      location = birminghamLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court request comments",
    )

    webTestClient.requestVideoLink(courtRequest)

    // There should be 2 notifications - one user email and 1 prison email
    val notifications = notificationRepository.findAll().also { it hasSize 2 }

    notifications.isPresent("j@j.com", "requested court booking prison template id with no email address")
    notifications.isPresent(PRISON_USER.email!!, "requested court booking user template id")
  }

  @Test
  fun `should fail to request a clashing court booking`() {
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

    webTestClient.createBooking(courtBookingRequest, COURT_USER)

    val courtRequest = requestCourtVideoLinkRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      firstName = "John",
      lastName = "Smith",
      dateOfBirth = LocalDate.of(1970, 1, 1),
      prisonCode = BIRMINGHAM,
      location = birminghamLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court request comments",
    )

    val error = webTestClient.post()
      .uri("/video-link-booking/request")
      .bodyValue(courtRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = COURT_USER.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
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
  fun `should request a Blackpool probation team booking and emails sent to Norwich prison`() {
    notificationRepository.findAll() hasSize 0

    locationsInsidePrisonApi().stubGetLocationByKey(norwichLocation.key, NORWICH)

    val probationRequest = requestProbationVideoLinkRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      firstName = "John",
      lastName = "Smith",
      dateOfBirth = LocalDate.of(1970, 1, 1),
      prisonCode = NORWICH,
      location = norwichLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test probation request comments",
    )

    webTestClient.requestVideoLink(probationRequest)

    // There should be 2 notifications - one user email and 1 prison email
    val notifications = notificationRepository.findAll().also { it hasSize 2 }

    notifications.isPresent("r@r.com", "requested probation booking prison template id with email address")
    notifications.isPresent(PRISON_USER.email!!, "requested probation booking user template id")
  }

  @Test
  fun `should request a Harrow probation booking and emails sent to Birmingham prison`() {
    notificationRepository.findAll() hasSize 0

    locationsInsidePrisonApi().stubGetLocationByKey(birminghamLocation.key, BIRMINGHAM)

    val probationRequest = requestProbationVideoLinkRequest(
      probationTeamCode = HARROW,
      firstName = "John",
      lastName = "Smith",
      dateOfBirth = LocalDate.of(1970, 1, 1),
      prisonCode = BIRMINGHAM,
      location = birminghamLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test probation request comments",
    )

    webTestClient.requestVideoLink(probationRequest)

    // There should be 2 notifications - one user email and 1 prison email
    val notifications = notificationRepository.findAll().also { it hasSize 2 }

    notifications.isPresent("j@j.com", "requested probation booking prison template id with no email address")
    notifications.isPresent(PRISON_USER.email!!, "requested probation booking user template id")
  }

  @Test
  fun `should fail to request a clashing probation booking`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)
    locationsInsidePrisonApi().stubGetLocationByKey(birminghamLocation.key, BIRMINGHAM)

    val probationBookingRequest = probationBookingRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      prisonerNumber = "123456",
      prisonCode = BIRMINGHAM,
      location = birminghamLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
    )

    webTestClient.createBooking(probationBookingRequest, PROBATION_USER)

    val probationRequest = requestProbationVideoLinkRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      firstName = "John",
      lastName = "Smith",
      dateOfBirth = LocalDate.of(1970, 1, 1),
      prisonCode = BIRMINGHAM,
      location = birminghamLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court request comments",
    )

    val error = webTestClient.post()
      .uri("/video-link-booking/request")
      .bodyValue(probationRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = PROBATION_USER.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
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
  @Sql("classpath:integration-test-data/seed-search-for-booking.sql")
  fun `should find matching court video link bookings`() {
    webTestClient.searchForBooking(
      VideoBookingSearchRequest(
        prisonerNumber = "123456",
        locationKey = "WNI-123456",
        date = today(),
        startTime = LocalTime.of(12, 0),
        endTime = LocalTime.of(13, 0),
      ),
      COURT_USER,
    ).videoLinkBookingId isEqualTo 1000

    webTestClient.searchForBooking(
      VideoBookingSearchRequest(
        prisonerNumber = "78910",
        locationKey = "WNI-78910",
        date = tomorrow(),
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(10, 0),
      ),
      COURT_USER,
    ).videoLinkBookingId isEqualTo 3000
  }

  private fun Int.messagesShouldBePublished(f: KArgumentCaptor<DomainEvent<*>>.() -> Unit) {
    waitForMessagesOnQueue(this)

    val times = this

    argumentCaptor<DomainEvent<*>> {
      verify(outboundEventsPublisher, org.mockito.kotlin.times(times)).send(capture())
      this.apply(f)
    }
  }

  private fun WebTestClient.createBookingFails(request: CreateVideoBookingRequest, user: User) =
    this
      .post()
      .uri("/video-link-booking")
      .bodyValue(request)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = user.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()

  private fun WebTestClient.searchForBooking(request: VideoBookingSearchRequest, user: User) =
    this
      .post()
      .uri("/video-link-booking/search")
      .bodyValue(request)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = user.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(VideoLinkBooking::class.java)
      .returnResult().responseBody!!

  private fun WebTestClient.amendBooking(videoBookingId: Long, request: AmendVideoBookingRequest, user: User) =
    this
      .put()
      .uri("/video-link-booking/id/$videoBookingId")
      .bodyValue(request)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = user.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Long::class.java)
      .returnResult().responseBody!!

  private fun WebTestClient.cancelBooking(videoBookingId: Long, user: User) =
    this
      .delete()
      .uri("/video-link-booking/id/$videoBookingId")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = user.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isNoContent

  private fun WebTestClient.requestVideoLink(request: RequestVideoBookingRequest) =
    this
      .post()
      .uri("/video-link-booking/request")
      .bodyValue(request)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = PRISON_USER.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isOk

  private fun WebTestClient.getBookingByIdRequest(videoBookingId: Long, user: User) =
    this
      .get()
      .uri("/video-link-booking/id/{videoBookinId}", videoBookingId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = user.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(VideoLinkBooking::class.java)
      .returnResult().responseBody!!

  private fun WebTestClient.getBookingByIdNotFound(videoBookingId: Long, user: User) =
    this
      .get()
      .uri("/video-link-booking/id/{videoBookinId}", videoBookingId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = user.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isNotFound
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

  private fun Collection<Notification>.isPresent(email: String, template: String, booking: VideoBooking? = null) {
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
        else -> throw RuntimeException("Unsupported email in test email configuration: ${email.javaClass.simpleName}")
      }
    }
}

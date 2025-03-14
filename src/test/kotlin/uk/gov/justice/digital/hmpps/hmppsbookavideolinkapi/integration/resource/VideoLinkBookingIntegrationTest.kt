package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Email
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TestEmailConfiguration
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationAttribute
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Notification
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.StatusCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BLACKPOOL_MC_PPOC
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.CHESTERFIELD_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.HARROW
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.LocationKeyValue
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.NORWICH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_RISLEY
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.RISLEY
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendCourtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendProbationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isInstanceOf
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isNotEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.norwichLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvilleLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.requestCourtVideoLinkRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.requestProbationVideoLinkRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.risleyLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CourtHearingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.RequestVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.VideoBookingSearchRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.BookingStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.VideoLinkBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingHistoryRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.LocationAttributeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.AmendedCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.AmendedCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.AmendedCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CourtBookingRequestPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CourtBookingRequestPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CourtBookingRequestUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.NewCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.NewCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.NewCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.NewCourtBookingUserEmail
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
import kotlin.reflect.KClass

@ContextConfiguration(classes = [TestEmailConfiguration::class])
class VideoLinkBookingIntegrationTest : SqsIntegrationTestBase() {

  @MockitoSpyBean
  private lateinit var outboundEventsPublisher: OutboundEventsPublisher

  private val eventCaptor = argumentCaptor<DomainEvent<*>>()

  @MockitoBean
  private lateinit var manageExternalAppointmentsService: ManageExternalAppointmentsService

  @Autowired
  private lateinit var videoBookingRepository: VideoBookingRepository

  @Autowired
  private lateinit var prisonAppointmentRepository: PrisonAppointmentRepository

  @Autowired
  private lateinit var notificationRepository: NotificationRepository

  @Autowired
  private lateinit var bookingHistoryRepository: BookingHistoryRepository

  @Autowired
  private lateinit var locationAttributeRepository: LocationAttributeRepository

  @Autowired
  private lateinit var prisonRepository: PrisonRepository

  @Test
  fun `should create a Derby court booking as court users and emails sent to Pentonville prison`() {
    videoBookingRepository.findAll() hasSize 0
    notificationRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", PENTONVILLE)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = PENTONVILLE,
      location = pentonvilleLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, COURT_USER)

    val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    with(persistedBooking) {
      videoBookingId isEqualTo bookingId
      bookingType isEqualTo BookingType.COURT
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
      prisonCode() isEqualTo PENTONVILLE
      prisonerNumber isEqualTo "123456"
      appointmentType isEqualTo AppointmentType.VLB_COURT_MAIN.name
      appointmentDate isEqualTo tomorrow()
      prisonLocationId isEqualTo pentonvilleLocation.id
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

    // There should be 4 notifications - 1 user email and 2 prison emails
    val notifications = notificationRepository.findAll().also { it hasSize 3 }

    notifications.isPresent("p@p.com", NewCourtBookingPrisonCourtEmail::class, persistedBooking)
    notifications.isPresent("g@g.com", NewCourtBookingPrisonCourtEmail::class, persistedBooking)
    notifications.isPresent(COURT_USER.email!!, NewCourtBookingUserEmail::class, persistedBooking)

    waitUntil {
      verify(outboundEventsPublisher, times(2)).send(eventCaptor.capture())
    }

    with(eventCaptor) {
      firstValue isInstanceOf VideoBookingCreatedEvent::class.java
      firstValue.additionalInformation isEqualTo VideoBookingInformation(persistedBooking.videoBookingId)

      secondValue isInstanceOf AppointmentCreatedEvent::class.java
      secondValue.additionalInformation isEqualTo AppointmentInformation(persistedAppointment.prisonAppointmentId)
    }

    waitUntil {
      verify(manageExternalAppointmentsService).createAppointment(persistedAppointment.prisonAppointmentId)
    }
  }

  @Test
  fun `should create a Derby court booking as prison user and emails sent to Pentonville prison and Derby court`() {
    val prisonUser = PRISON_USER_PENTONVILLE.also(::stubUser)

    videoBookingRepository.findAll() hasSize 0
    notificationRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", PENTONVILLE)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = PENTONVILLE,
      location = pentonvilleLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, prisonUser)

    val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    with(persistedBooking) {
      videoBookingId isEqualTo bookingId
      bookingType isEqualTo BookingType.COURT
      court?.code isEqualTo courtBookingRequest.courtCode
      hearingType isEqualTo courtBookingRequest.courtHearingType?.name
      comments isEqualTo "integration test court booking comments"
      videoUrl isEqualTo courtBookingRequest.videoLinkUrl
      createdBy isEqualTo PRISON_USER_PENTONVILLE.username
      createdByPrison isEqualTo true
    }

    val persistedAppointment = prisonAppointmentRepository.findByVideoBooking(persistedBooking).single()

    with(persistedAppointment) {
      videoBooking isEqualTo persistedBooking
      prisonCode() isEqualTo PENTONVILLE
      prisonerNumber isEqualTo "123456"
      appointmentType isEqualTo AppointmentType.VLB_COURT_MAIN.name
      appointmentDate isEqualTo tomorrow()
      prisonLocationId isEqualTo pentonvilleLocation.id
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

    // There should be 5 notifications - 1 user email, 2 court emails and 2 prison emails
    val notifications = notificationRepository.findAll().also { it hasSize 5 }

    notifications.isPresent("p@p.com", NewCourtBookingPrisonCourtEmail::class, persistedBooking)
    notifications.isPresent("g@g.com", NewCourtBookingPrisonCourtEmail::class, persistedBooking)
    notifications.isPresent("j@j.com", NewCourtBookingCourtEmail::class, persistedBooking)
    notifications.isPresent("b@b.com", NewCourtBookingCourtEmail::class, persistedBooking)
    notifications.isPresent(prisonUser.email!!, NewCourtBookingUserEmail::class, persistedBooking)

    waitUntil {
      verify(outboundEventsPublisher, times(2)).send(eventCaptor.capture())
    }

    with(eventCaptor) {
      firstValue isInstanceOf VideoBookingCreatedEvent::class.java
      firstValue.additionalInformation isEqualTo VideoBookingInformation(persistedBooking.videoBookingId)

      secondValue isInstanceOf AppointmentCreatedEvent::class.java
      secondValue.additionalInformation isEqualTo AppointmentInformation(persistedAppointment.prisonAppointmentId)
    }

    waitUntil {
      verify(manageExternalAppointmentsService).createAppointment(persistedAppointment.prisonAppointmentId)
    }
  }

  @Test
  fun `should allow creation of overlapping court bookings as a prison user`() {
    val prisonUser = PRISON_USER_PENTONVILLE.also(::stubUser)

    videoBookingRepository.findAll() hasSize 0
    notificationRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", PENTONVILLE)
    prisonSearchApi().stubGetPrisoner("789101", PENTONVILLE)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = PENTONVILLE,
      location = pentonvilleLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    webTestClient.createBooking(courtBookingRequest, prisonUser)

    val overlappingCourtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "789101",
      prisonCode = PENTONVILLE,
      location = pentonvilleLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    // No error should be thrown
    webTestClient.createBooking(overlappingCourtBookingRequest, prisonUser)
  }

  @Test
  fun `should reject duplicate court booking creation as a prison user`() {
    val prisonUser = PRISON_USER_PENTONVILLE.also(::stubUser)

    videoBookingRepository.findAll() hasSize 0
    notificationRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", PENTONVILLE)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = PENTONVILLE,
      location = pentonvilleLocation,
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
      bookingType isEqualTo BookingType.COURT
      court?.code isEqualTo courtBookingRequest.courtCode
      hearingType isEqualTo courtBookingRequest.courtHearingType?.name
      comments isEqualTo "integration test court booking comments"
      videoUrl isEqualTo courtBookingRequest.videoLinkUrl
      createdBy isEqualTo COURT_USER.username
      createdByPrison isEqualTo false
    }

    with(prisonAppointmentRepository.findByVideoBooking(persistedBooking).single()) {
      videoBooking isEqualTo persistedBooking
      prisonCode() isEqualTo BIRMINGHAM
      prisonerNumber isEqualTo "123456"
      appointmentType isEqualTo AppointmentType.VLB_COURT_MAIN.name
      appointmentDate isEqualTo tomorrow()
      prisonLocationId isEqualTo birminghamLocation.id
      startTime isEqualTo LocalTime.of(12, 0)
      endTime isEqualTo LocalTime.of(12, 30)
      comments isEqualTo "integration test court booking comments"
    }

    // There should be 2 - notifications one user email and 1 prison email
    val notifications = notificationRepository.findAll().also { it hasSize 2 }

    notifications.isPresent("a@a.com", NewCourtBookingPrisonNoCourtEmail::class, persistedBooking)
    notifications.isPresent(COURT_USER.email!!, NewCourtBookingUserEmail::class, persistedBooking)
  }

  @Test
  fun `should fail to create a clashing court booking as court user`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)

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
      userMessage isEqualTo "Exception: Unable to create court booking, booking overlaps with an existing appointment."
      developerMessage isEqualTo "Unable to create court booking, booking overlaps with an existing appointment."
    }
  }

  @Test
  fun `should create a court booking which clashes with a future cancelled booking as court user`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)

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

    prisonSearchApi().stubGetPrisoner("123456", WANDSWORTH)

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
  fun `should fail to create a court booking when prison is not enabled for self service as court user`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", RISLEY)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = RISLEY,
      location = risleyLocation,
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
      userMessage isEqualTo "Exception: Prison with code RSI is not enabled for self service"
      developerMessage isEqualTo "Prison with code RSI is not enabled for self service"
    }
  }

  @Test
  fun `should create a probation booking as probation user`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)

    val probationBookingRequest = probationBookingRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      probationMeetingType = ProbationMeetingType.PSR,
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
      bookingType isEqualTo BookingType.PROBATION
      probationTeam?.probationTeamId isEqualTo 1
      probationMeetingType isEqualTo ProbationMeetingType.PSR.name
      comments isEqualTo "integration test probation booking comments"
      videoUrl isEqualTo null
      createdBy isEqualTo PROBATION_USER.username
      createdByPrison isEqualTo false
    }

    with(prisonAppointmentRepository.findByVideoBooking(persistedBooking).single()) {
      videoBooking isEqualTo persistedBooking
      prisonCode() isEqualTo BIRMINGHAM
      prisonerNumber isEqualTo "123456"
      appointmentType isEqualTo AppointmentType.VLB_PROBATION.name
      appointmentDate isEqualTo tomorrow()
      prisonLocationId isEqualTo birminghamLocation.id
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

    val probationBookingRequest = probationBookingRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      probationMeetingType = ProbationMeetingType.PSR,
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
  fun `should fail to create a overlapping probation booking as probation user`() {
    prisonSearchApi().stubGetPrisoner("123456", WANDSWORTH)

    val probationBookingRequest = probationBookingRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      probationMeetingType = ProbationMeetingType.PSR,
      prisonCode = WANDSWORTH,
      prisonerNumber = "123456",
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(9, 30),
      appointmentType = AppointmentType.VLB_PROBATION,
      location = wandsworthLocation,
    )

    webTestClient.createBooking(probationBookingRequest, PROBATION_USER)

    prisonSearchApi().stubGetPrisoner("789012", WANDSWORTH)

    val clashingBookingRequest = probationBookingRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      probationMeetingType = ProbationMeetingType.PSR,
      prisonCode = WANDSWORTH,
      prisonerNumber = "789012",
      startTime = LocalTime.of(8, 0),
      endTime = LocalTime.of(9, 30),
      appointmentType = AppointmentType.VLB_PROBATION,
      location = wandsworthLocation,
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
      userMessage isEqualTo "Exception: Unable to create probation booking, booking overlaps with an existing appointment."
      developerMessage isEqualTo "Unable to create probation booking, booking overlaps with an existing appointment."
    }
  }

  @Test
  fun `should fail to create a probation booking when prisoner not at prison`() {
    prisonSearchApi().stubGetPrisoner("789012", BIRMINGHAM)

    val clashingBookingRequest = probationBookingRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      probationMeetingType = ProbationMeetingType.PSR,
      prisonCode = WANDSWORTH,
      prisonerNumber = "789012",
      startTime = LocalTime.of(8, 0),
      endTime = LocalTime.of(9, 30),
      appointmentType = AppointmentType.VLB_PROBATION,
      location = wandsworthLocation,
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
      userMessage isEqualTo "Validation failure: Prisoner 789012 not found at prison WWI"
      developerMessage isEqualTo "Prisoner 789012 not found at prison WWI"
    }
  }

  @Test
  fun `should fail to create a probation booking when prison is not enabled for self service`() {
    prisonSearchApi().stubGetPrisoner("789012", RISLEY)

    val clashingBookingRequest = probationBookingRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      probationMeetingType = ProbationMeetingType.PSR,
      prisonCode = RISLEY,
      prisonerNumber = "789012",
      startTime = LocalTime.of(8, 0),
      endTime = LocalTime.of(9, 30),
      appointmentType = AppointmentType.VLB_PROBATION,
      location = risleyLocation,
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
      userMessage isEqualTo "Exception: Prison with code RSI is not enabled for self service"
      developerMessage isEqualTo "Prison with code RSI is not enabled for self service"
    }
  }

  @Test
  fun `should fail to create a probation booking when user is prison user`() {
    val error = webTestClient.createBookingFails(probationBookingRequest(), PRISON_USER_BIRMINGHAM)
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

    prisonSearchApi().stubGetPrisoner("123456", PENTONVILLE)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = PENTONVILLE,
      location = pentonvilleLocation,
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
        assertThat(prisonCode).isEqualTo(PENTONVILLE)
        assertThat(prisonLocKey).isEqualTo(pentonvilleLocation.key)
      }
    }
  }

  @Test
  fun `should return the details of a court video link booking by ID when prison is not self service for prison user`() {
    val prisonUser = PRISON_USER_RISLEY.also(::stubUser)
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", RISLEY)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = RISLEY,
      location = risleyLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, prisonUser)

    assertThat(bookingId).isGreaterThan(0L)

    val bookingDetails = webTestClient.getBookingByIdRequest(bookingId, prisonUser)

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

      assertThat(createdByPrison).isTrue()
      assertThat(videoLinkUrl).isEqualTo("https://video.link.com")

      // Verify that there is a single appointment
      assertThat(prisonAppointments).hasSize(1)
      with(prisonAppointments.first()) {
        assertThat(appointmentType).isEqualTo("VLB_COURT_MAIN")
        assertThat(comments).contains("integration test")
        assertThat(prisonCode).isEqualTo(RISLEY)
        assertThat(prisonLocKey).isEqualTo(risleyLocation.key)
      }
    }
  }

  @Test
  fun `should fail to return the details of a court video link booking by ID if the prison is not self service for court user`() {
    val prisonUser = PRISON_USER_RISLEY.also(::stubUser)
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", RISLEY)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = RISLEY,
      location = risleyLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, prisonUser)

    assertThat(bookingId).isGreaterThan(0L)

    val error = webTestClient.get()
      .uri("/video-link-booking/id/{videoBookingId}", bookingId)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = COURT_USER.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().is4xxClientError
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    with(error) {
      status isEqualTo 404
      userMessage isEqualTo "Not found: Prison with code RSI for booking with id 1 is not self service"
      developerMessage isEqualTo "Prison with code RSI for booking with id 1 is not self service"
    }
  }

  @Test
  fun `should return the details of a probation video link booking by ID`() {
    videoBookingRepository.findAll() hasSize 0

    locationAttributeRepository.saveAndFlush(
      LocationAttribute.decoratedRoom(
        dpsLocationId = wandsworthLocation.id,
        prison = prisonRepository.findByCode(WANDSWORTH)!!,
        createdBy = PROBATION_USER,
        locationUsage = LocationUsage.PROBATION,
        locationStatus = LocationStatus.ACTIVE,
        prisonVideoUrl = "https://probation-url",
        notes = null,
        allowedParties = emptySet(),
      ),
    )

    prisonSearchApi().stubGetPrisoner("123456", WANDSWORTH)
    nomisMappingApi().stubGetNomisLocationMappingBy(wandsworthLocation, 1)
    prisonApi().stubGetScheduledAppointments(WANDSWORTH, tomorrow(), 1)
    locationsInsidePrisonApi().stubGetLocationById(wandsworthLocation)

    val probationBookingRequest = probationBookingRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      probationMeetingType = ProbationMeetingType.PSR,
      prisonerNumber = "123456",
      prisonCode = WANDSWORTH,
      location = wandsworthLocation,
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
      assertThat(probationTeamDescription).isEqualTo("Blackpool Magistrates - Probation")
      assertThat(probationMeetingType).isEqualTo(ProbationMeetingType.PSR)
      assertThat(probationMeetingTypeDescription).isEqualTo("Pre-sentence report")

      // Verify court details are null
      assertThat(courtCode).isNull()
      assertThat(courtDescription).isNull()
      assertThat(courtHearingType).isNull()
      assertThat(courtHearingTypeDescription).isNull()

      assertThat(createdByPrison).isFalse()
      assertThat(videoLinkUrl).isEqualTo("https://probation-url")

      // Verify that there is a single appointment
      assertThat(prisonAppointments).hasSize(1)
      with(prisonAppointments.first()) {
        assertThat(appointmentType).isEqualTo("VLB_PROBATION")
        assertThat(comments).contains("integration test")
        assertThat(prisonCode).isEqualTo(WANDSWORTH)
        assertThat(prisonLocKey).isEqualTo(wandsworthLocation.key)
      }
    }
  }

  @Test
  fun `should return a 404 not found when requesting an invalid video booking ID`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", PENTONVILLE)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = PENTONVILLE,
      location = pentonvilleLocation,
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
  fun `should amend a Derby court booking and emails sent to Pentonville prison`() {
    videoBookingRepository.findAll() hasSize 0
    notificationRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", PENTONVILLE)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = PENTONVILLE,
      location = pentonvilleLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, COURT_USER)

    val amendBookingRequest = amendCourtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = PENTONVILLE,
      location = pentonvilleLocation,
      startTime = LocalTime.of(13, 0),
      endTime = LocalTime.of(14, 30),
      comments = "amended court booking comments",
    )

    notificationRepository.deleteAll()
    webTestClient.amendBooking(bookingId, amendBookingRequest, COURT_USER)

    val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    with(persistedBooking) {
      videoBookingId isEqualTo bookingId
      bookingType isEqualTo BookingType.COURT
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
      prisonCode() isEqualTo PENTONVILLE
      prisonerNumber isEqualTo "123456"
      appointmentType isEqualTo AppointmentType.VLB_COURT_MAIN.name
      appointmentDate isEqualTo tomorrow()
      prisonLocationId isEqualTo pentonvilleLocation.id
      startTime isEqualTo LocalTime.of(13, 0)
      endTime isEqualTo LocalTime.of(14, 30)
      comments isEqualTo "amended court booking comments"
    }

    // There should be 3 notifications, 1 user email and 2 prison emails
    val notifications = notificationRepository.findAll().also { it hasSize 3 }

    notifications.isPresent("p@p.com", AmendedCourtBookingPrisonCourtEmail::class, persistedBooking)
    notifications.isPresent("g@g.com", AmendedCourtBookingPrisonCourtEmail::class, persistedBooking)
    notifications.isPresent(COURT_USER.email!!, AmendedCourtBookingUserEmail::class, persistedBooking)
  }

  @Test
  fun `should amend a Chesterfield court booking and emails sent to Birmingham prison`() {
    videoBookingRepository.findAll() hasSize 0
    notificationRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)

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
      bookingType isEqualTo BookingType.COURT
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
      prisonCode() isEqualTo BIRMINGHAM
      prisonerNumber isEqualTo "123456"
      appointmentType isEqualTo AppointmentType.VLB_COURT_MAIN.name
      appointmentDate isEqualTo tomorrow()
      prisonLocationId isEqualTo birminghamLocation.id
      startTime isEqualTo LocalTime.of(13, 0)
      endTime isEqualTo LocalTime.of(14, 30)
      comments isEqualTo "amended court booking comments"
    }

    // There should be 2 notifications - one user email and 1 prisoner email
    val notifications = notificationRepository.findAll().also { it hasSize 2 }

    notifications.isPresent("a@a.com", AmendedCourtBookingPrisonNoCourtEmail::class, persistedBooking)
    notifications.isPresent(COURT_USER.email!!, AmendedCourtBookingUserEmail::class, persistedBooking)
  }

  @Test
  fun `should fail to amend to a clashing court booking`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)

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
      userMessage isEqualTo "Exception: Unable to amend court booking, booking overlaps with an existing appointment."
      developerMessage isEqualTo "Unable to amend court booking, booking overlaps with an existing appointment."
    }
  }

  @Sql("classpath:integration-test-data/seed-video-booking-user-preferences.sql")
  @Test
  fun `should fail to amend a court booking when prisoner not at prison`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = BIRMINGHAM,
      location = birminghamLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
    )

    val videoBookingId = webTestClient.createBooking(courtBookingRequest, COURT_USER)

    prisonSearchApi().stubGetPrisoner("123456", WANDSWORTH)

    val locationId = UUID.randomUUID()
    locationsInsidePrisonApi().stubPostLocationByKeys(listOf(LocationKeyValue(birminghamLocation.key, locationId)), BIRMINGHAM)

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
  @Sql("classpath:integration-test-data/seed-bookings-happening-tomorrow.sql")
  fun `should fail to amend a court booking when the prison is not enabled for self service as court user`() {
    prisonSearchApi().stubGetPrisoner("123456", RISLEY)

    val amendBookingRequest = amendCourtBookingRequest(
      prisonCode = RISLEY,
      prisonerNumber = "123456",
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(9, 30),
      location = risleyLocation,
      comments = "integration test probation booking comments",
    )

    val error = webTestClient.put()
      .uri("/video-link-booking/id/1000")
      .bodyValue(amendBookingRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = COURT_USER.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().is4xxClientError
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    with(error) {
      status isEqualTo 404
      userMessage isEqualTo "Not found: Prison with code RSI for booking with id 1000 is not self service"
      developerMessage isEqualTo "Prison with code RSI for booking with id 1000 is not self service"
    }
  }

  @Test
  fun `should be able to create and amend a court booking when the prison is not enabled as prison user`() {
    val prisonUser = PRISON_USER_RISLEY.also(::stubUser)
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", RISLEY)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = RISLEY,
      location = risleyLocation,
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
    )

    val videoBookingId = webTestClient.createBooking(courtBookingRequest, prisonUser)

    val amendBookingRequest = amendCourtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = RISLEY,
      location = risleyLocation,
      startTime = LocalTime.of(13, 0),
      endTime = LocalTime.of(14, 30),
    )

    webTestClient.amendBooking(videoBookingId, amendBookingRequest, prisonUser)

    val persistedBooking = videoBookingRepository.findById(videoBookingId).orElseThrow()

    with(prisonAppointmentRepository.findByVideoBooking(persistedBooking).single()) {
      videoBooking isEqualTo persistedBooking
      prisonCode() isEqualTo RISLEY
      startTime isEqualTo LocalTime.of(13, 0)
      endTime isEqualTo LocalTime.of(14, 30)
    }
  }

  @Test
  fun `should amend a probation booking`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)

    val probationBookingRequest = probationBookingRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      probationMeetingType = ProbationMeetingType.PSR,
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
      bookingType isEqualTo BookingType.PROBATION
      probationTeam?.probationTeamId isEqualTo 1
      probationMeetingType isEqualTo ProbationMeetingType.PSR.name
      comments isEqualTo "amended probation booking comments"
      videoUrl isEqualTo null
      createdBy isEqualTo PROBATION_USER.username
      createdByPrison isEqualTo false
      amendedBy isEqualTo PROBATION_USER.username
    }

    with(prisonAppointmentRepository.findByVideoBooking(persistedBooking).single()) {
      videoBooking isEqualTo persistedBooking
      prisonCode() isEqualTo BIRMINGHAM
      prisonerNumber isEqualTo "123456"
      appointmentType isEqualTo AppointmentType.VLB_PROBATION.name
      appointmentDate isEqualTo tomorrow()
      prisonLocationId isEqualTo birminghamLocation.id
      startTime isEqualTo LocalTime.of(10, 0)
      endTime isEqualTo LocalTime.of(11, 30)
      comments isEqualTo "amended probation booking comments"
    }
  }

  @Test
  fun `should fail to amend to a clashing probation booking`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)

    val probationBookingRequest1 = probationBookingRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      probationMeetingType = ProbationMeetingType.PSR,
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
      userMessage isEqualTo "Exception: Unable to amend probation booking, booking overlaps with an existing appointment."
      developerMessage isEqualTo "Unable to amend probation booking, booking overlaps with an existing appointment."
    }
  }

  @Test
  fun `should fail to amend a probation booking when prisoner not at prison`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)

    val probationBookingRequest = probationBookingRequest(
      probationTeamCode = BLACKPOOL_MC_PPOC,
      probationMeetingType = ProbationMeetingType.PSR,
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(9, 30),
      appointmentType = AppointmentType.VLB_PROBATION,
      location = birminghamLocation,
      comments = "integration test probation booking comments",
    )

    val videoBookingId = webTestClient.createBooking(probationBookingRequest, PROBATION_USER)

    prisonSearchApi().stubGetPrisoner("123456", WANDSWORTH)

    val amendBookingRequest = amendProbationBookingRequest(
      probationMeetingType = ProbationMeetingType.PSR,
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
  @Sql("classpath:integration-test-data/seed-bookings-happening-tomorrow.sql")
  fun `should fail to amend a probation booking when the prison is not enabled for self service`() {
    prisonSearchApi().stubGetPrisoner("123456", RISLEY)

    val amendBookingRequest = amendProbationBookingRequest(
      probationMeetingType = ProbationMeetingType.PSR,
      prisonCode = RISLEY,
      prisonerNumber = "123456",
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(9, 30),
      appointmentType = AppointmentType.VLB_PROBATION,
      location = risleyLocation,
      comments = "integration test probation booking comments",
    )

    val error = webTestClient.put()
      .uri("/video-link-booking/id/1001")
      .bodyValue(amendBookingRequest)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(user = PROBATION_USER.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().is4xxClientError
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody!!

    with(error) {
      status isEqualTo 404
      userMessage isEqualTo "Not found: Prison with code RSI for booking with id 1001 is not self service"
      developerMessage isEqualTo "Prison with code RSI for booking with id 1001 is not self service"
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

    waitUntil {
      verify(outboundEventsPublisher, times(3)).send(eventCaptor.capture())
    }

    with(eventCaptor) {
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

    webTestClient.requestVideoLink(courtRequest, COURT_USER)

    // There should be 2 notifications - 1 user email and 1 prison email
    val notifications = notificationRepository.findAll().also { it hasSize 2 }

    notifications.isPresent("r@r.com", CourtBookingRequestPrisonCourtEmail::class)
    notifications.isPresent(COURT_USER.email!!, CourtBookingRequestUserEmail::class)
  }

  @Test
  fun `should request a Chesterfield court booking and emails sent to Birmingham prison`() {
    notificationRepository.findAll() hasSize 0

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

    webTestClient.requestVideoLink(courtRequest, COURT_USER)

    // There should be 2 notifications - one user email and 1 prison email
    val notifications = notificationRepository.findAll().also { it hasSize 2 }

    notifications.isPresent("a@a.com", CourtBookingRequestPrisonNoCourtEmail::class)
    notifications.isPresent(COURT_USER.email!!, CourtBookingRequestUserEmail::class)
  }

  @Test
  fun `should fail to request a clashing court booking`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)

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
      userMessage isEqualTo "Exception: Unable to request court booking, booking overlaps with an existing appointment."
      developerMessage isEqualTo "Unable to request court booking, booking overlaps with an existing appointment."
    }
  }

  @Test
  fun `should request a Blackpool probation team booking and emails sent to Norwich prison`() {
    notificationRepository.findAll() hasSize 0

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

    webTestClient.requestVideoLink(probationRequest, PROBATION_USER)

    // There should be 2 notifications - one user email and 1 prison email
    val notifications = notificationRepository.findAll().also { it hasSize 2 }

    notifications.isPresent("r@r.com", ProbationBookingRequestPrisonProbationTeamEmail::class)
    notifications.isPresent(PROBATION_USER.email!!, ProbationBookingRequestUserEmail::class)
  }

  @Test
  fun `should request a Harrow probation booking and emails sent to Birmingham prison`() {
    notificationRepository.findAll() hasSize 0

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

    webTestClient.requestVideoLink(probationRequest, PROBATION_USER)

    // There should be 2 notifications - one user email and 1 prison email
    val notifications = notificationRepository.findAll().also { it hasSize 2 }

    notifications.isPresent("a@a.com", ProbationBookingRequestPrisonNoProbationTeamEmail::class)
    notifications.isPresent(PROBATION_USER.email!!, ProbationBookingRequestUserEmail::class)
  }

  @Test
  fun `should fail to request a clashing probation booking`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", BIRMINGHAM)

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
      userMessage isEqualTo "Exception: Unable to request probation booking, booking overlaps with an existing appointment."
      developerMessage isEqualTo "Unable to request probation booking, booking overlaps with an existing appointment."
    }
  }

  @Test
  @Sql("classpath:integration-test-data/seed-search-for-booking.sql")
  fun `should find matching court video link bookings`() {
    val location1 = pentonvilleLocation.copy(id = UUID.fromString("b13f9018-f22d-456f-a690-d80e3d0feb5f"))
    locationsInsidePrisonApi().stubGetLocationByKey(location1)
    locationsInsidePrisonApi().stubGetLocationById(location1)

    webTestClient.searchForBooking(
      VideoBookingSearchRequest(
        prisonerNumber = "123456",
        locationKey = location1.key,
        date = today(),
        startTime = LocalTime.of(12, 0),
        endTime = LocalTime.of(13, 0),
      ),
      COURT_USER,
    ).videoLinkBookingId isEqualTo 1000

    val location2 = pentonvilleLocation.copy(id = UUID.fromString("ba0df03b-7864-47d5-9729-0301b74ecbe2"), key = "PVI-78910")
    locationsInsidePrisonApi().stubGetLocationByKey(location2)
    locationsInsidePrisonApi().stubGetLocationById(location2)

    webTestClient.searchForBooking(
      VideoBookingSearchRequest(
        prisonerNumber = "78910",
        locationKey = location2.key,
        date = tomorrow(),
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(10, 0),
      ),
      COURT_USER,
    ).videoLinkBookingId isEqualTo 3000
  }

  @Test
  @Sql("classpath:integration-test-data/seed-search-for-booking.sql")
  fun `should find matching CANCELLED court video link bookings`() {
    val location = pentonvilleLocation.copy(id = UUID.fromString("ba0df03b-7864-47d5-9729-0301b74ecbe2"), key = "PVI-78910")
    locationsInsidePrisonApi().stubGetLocationByKey(location)
    locationsInsidePrisonApi().stubGetLocationById(location)

    webTestClient.searchForBooking(
      VideoBookingSearchRequest(
        prisonerNumber = "78910",
        locationKey = location.key,
        date = tomorrow(),
        startTime = LocalTime.of(9, 0),
        endTime = LocalTime.of(10, 0),
        statusCode = BookingStatus.CANCELLED,
      ),
      COURT_USER,
    ).videoLinkBookingId isEqualTo 4000
  }

  private fun WebTestClient.createBookingFails(request: CreateVideoBookingRequest, user: User) = this
    .post()
    .uri("/video-link-booking")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(user = user.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
    .exchange()

  private fun WebTestClient.searchForBooking(request: VideoBookingSearchRequest, user: User) = this
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

  private fun WebTestClient.amendBooking(videoBookingId: Long, request: AmendVideoBookingRequest, user: User) = this
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

  private fun WebTestClient.cancelBooking(videoBookingId: Long, user: User) = this
    .delete()
    .uri("/video-link-booking/id/$videoBookingId")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(user = user.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
    .exchange()
    .expectStatus().isNoContent

  private fun WebTestClient.requestVideoLink(request: RequestVideoBookingRequest, externalUser: ExternalUser) = this
    .post()
    .uri("/video-link-booking/request")
    .bodyValue(request)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(user = externalUser.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
    .exchange()
    .expectStatus().isOk

  private fun WebTestClient.getBookingByIdRequest(videoBookingId: Long, user: User) = this
    .get()
    .uri("/video-link-booking/id/{videoBookingId}", videoBookingId)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(user = user.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(VideoLinkBooking::class.java)
    .returnResult().responseBody!!

  private fun WebTestClient.getBookingByIdNotFound(videoBookingId: Long, user: User) = this
    .get()
    .uri("/video-link-booking/id/{videoBookingId}", videoBookingId)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(user = user.username, roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
    .exchange()
    .expectStatus().isNotFound
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(ErrorResponse::class.java)
    .returnResult().responseBody!!

  private fun <T : Email> Collection<Notification>.isPresent(email: String, template: KClass<T>, booking: VideoBooking? = null) {
    single { it.email == email && it.templateName == template.simpleName && it.videoBooking == booking }
  }
}

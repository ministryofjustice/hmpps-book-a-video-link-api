package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Email
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TestEmailConfiguration
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Notification
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.StatusCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BLACKPOOL_MC_PPOC
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.SERVICE_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsEntriesExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvilleLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.Appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.ProbationMeetingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingHistoryAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CancelledCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CancelledCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.NewCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.NewCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.ReleasedCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.ReleasedCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.TransferredCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.TransferredCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.CancelledProbationBookingPrisonProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.CancelledProbationBookingProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.NewProbationBookingPrisonProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.NewProbationBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.AppointmentScheduleInformation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.AppointmentsChangedInformation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.Identifier
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.InboundEventsListener
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.MergeInformation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PersonReference
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PrisonerAppointmentsChangedEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PrisonerMergedEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PrisonerReleasedEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.PrisonerVideoAppointmentCancelledEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.ReleaseInformation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.PrisonerMergedTelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.TelemetryService
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.reflect.KClass

@ContextConfiguration(classes = [TestEmailConfiguration::class])
class InboundEventsIntegrationTest : SqsIntegrationTestBase() {

  @MockitoSpyBean
  private lateinit var inboundEventsListener: InboundEventsListener

  @MockitoBean
  private lateinit var outboundEventsService: OutboundEventsService

  @Autowired
  private lateinit var videoBookingRepository: VideoBookingRepository

  @Autowired
  private lateinit var prisonAppointmentRepository: PrisonAppointmentRepository

  @Autowired
  private lateinit var bookingHistoryAppointmentRepository: BookingHistoryAppointmentRepository

  @Autowired
  private lateinit var notificationRepository: NotificationRepository

  @MockitoBean
  private lateinit var telemetryService: TelemetryService

  private val telemetryCaptor = argumentCaptor<PrisonerMergedTelemetryEvent>()

  @Test
  fun `should cancel a video booking and send release emails on receipt of a appointments changed event and last movement REL`() {
    videoBookingRepository.findAll() hasSize 0
    prisonSearchApi().stubGetPrisoner("123456", prisonCode = PENTONVILLE, lastPrisonCode = PENTONVILLE, lastMovementTypeCode = "REL")

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = PENTONVILLE,
      location = pentonvilleLocation,
      date = tomorrow(),
      startTime = LocalTime.of(9, 0),
      endTime = LocalTime.of(9, 30),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, COURT_USER)

    videoBookingRepository.findById(bookingId).orElseThrow().statusCode isEqualTo StatusCode.ACTIVE

    inboundEventsListener.onMessage(
      raw(
        PrisonerAppointmentsChangedEvent(
          personReference = PersonReference(listOf(Identifier("NOMS", "123456"))),
          additionalInformation = AppointmentsChangedInformation(
            action = "YES",
            prisonId = PENTONVILLE,
            user = "SOME_USER",
          ),
        ),
      ),
    )

    val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow().also { it.statusCode isEqualTo StatusCode.CANCELLED }

    with(notificationRepository.findAll()) {
      isPresent("b@b.com", ReleasedCourtBookingCourtEmail::class, persistedBooking)
      isPresent("j@j.com", ReleasedCourtBookingCourtEmail::class, persistedBooking)
      isPresent("g@g.com", ReleasedCourtBookingPrisonCourtEmail::class, persistedBooking)
      isPresent("p@p.com", ReleasedCourtBookingPrisonCourtEmail::class, persistedBooking)
    }
  }

  @Test
  fun `should cancel a video booking and send transfer emails on receipt of a appointments changed event and last movement TRN`() {
    videoBookingRepository.findAll() hasSize 0
    prisonSearchApi().stubGetPrisoner("123456", prisonCode = PENTONVILLE, lastPrisonCode = PENTONVILLE, lastMovementTypeCode = "TRN")

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = PENTONVILLE,
      location = pentonvilleLocation,
      date = tomorrow(),
      startTime = LocalTime.of(9, 30),
      endTime = LocalTime.of(20, 0),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, COURT_USER)

    videoBookingRepository.findById(bookingId).orElseThrow().statusCode isEqualTo StatusCode.ACTIVE

    inboundEventsListener.onMessage(
      raw(
        PrisonerAppointmentsChangedEvent(
          personReference = PersonReference(listOf(Identifier("NOMS", "123456"))),
          additionalInformation = AppointmentsChangedInformation(
            action = "YES",
            prisonId = PENTONVILLE,
            user = "SOME_USER",
          ),
        ),
      ),
    )

    val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow().also { it.statusCode isEqualTo StatusCode.CANCELLED }

    with(notificationRepository.findAll()) {
      isPresent("b@b.com", TransferredCourtBookingCourtEmail::class, persistedBooking)
      isPresent("j@j.com", TransferredCourtBookingCourtEmail::class, persistedBooking)
      isPresent("g@g.com", TransferredCourtBookingPrisonCourtEmail::class, persistedBooking)
      isPresent("p@p.com", TransferredCourtBookingPrisonCourtEmail::class, persistedBooking)
    }
  }

  @Test
  fun `should cancel a video booking on receipt of a permanent release event`() {
    videoBookingRepository.findAll() hasSize 0
    prisonSearchApi().stubGetPrisoner("123456", prisonCode = PENTONVILLE, lastPrisonCode = PENTONVILLE)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = PENTONVILLE,
      location = pentonvilleLocation,
      date = tomorrow(),
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, COURT_USER)

    videoBookingRepository.findById(bookingId).orElseThrow().statusCode isEqualTo StatusCode.ACTIVE

    inboundEventsListener.onMessage(
      raw(
        PrisonerReleasedEvent(
          ReleaseInformation(
            nomsNumber = "123456",
            prisonId = PENTONVILLE,
            reason = "RELEASED",
          ),
        ).also { it.isPermanent() isBool true },
      ),
    )

    videoBookingRepository.findById(bookingId).orElseThrow().statusCode isEqualTo StatusCode.CANCELLED
  }

  @Test
  fun `should not cancel a video booking on receipt of a temporary release event`() {
    videoBookingRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", PENTONVILLE)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = PENTONVILLE,
      location = pentonvilleLocation,
      date = tomorrow(),
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, COURT_USER)

    videoBookingRepository.findById(bookingId).orElseThrow().statusCode isEqualTo StatusCode.ACTIVE

    inboundEventsListener.onMessage(
      raw(
        PrisonerReleasedEvent(
          ReleaseInformation(
            nomsNumber = "123456",
            prisonId = PENTONVILLE,
            reason = "TEMPORARY_ABSENCE_RELEASE",
          ),
        ).also { it.isTemporary() isBool true },
      ),
    )

    videoBookingRepository.findById(bookingId).orElseThrow().statusCode isEqualTo StatusCode.ACTIVE
  }

  @Sql("classpath:integration-test-data/seed-historic-booking.sql")
  @Test
  fun `should not cancel historic video booking on receipt of a permanent release event`() {
    val historicBooking = videoBookingRepository.findById(-3).orElseThrow()

    prisonSearchApi().stubGetPrisoner("ABCDEF", PENTONVILLE)

    inboundEventsListener.onMessage(
      raw(
        PrisonerReleasedEvent(
          ReleaseInformation(
            nomsNumber = "ABCDEF",
            prisonId = PENTONVILLE,
            reason = "RELEASED",
          ),
        ).also { it.isPermanent() isBool true },
      ),
    )

    videoBookingRepository.findById(historicBooking.videoBookingId).orElseThrow().statusCode isEqualTo StatusCode.ACTIVE
  }

  @Test
  fun `should not attempt to cancel an already cancelled future booking`() {
    prisonSearchApi().stubGetPrisoner("YD1234", PENTONVILLE)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "YD1234",
      prisonCode = PENTONVILLE,
      location = pentonvilleLocation,
      date = tomorrow(),
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, COURT_USER)

    prisonSearchApi().stubGetPrisoner(prisonerNumber = "YD1234", prisonCode = "TRN", lastPrisonCode = PENTONVILLE)

    inboundEventsListener.onMessage(
      raw(
        PrisonerReleasedEvent(
          ReleaseInformation(
            nomsNumber = "YD1234",
            prisonId = "TRN",
            reason = "TRANSFERRED",
          ),
        ),
      ),
    )

    videoBookingRepository.findById(bookingId).orElseThrow().statusCode isEqualTo StatusCode.CANCELLED

    assertDoesNotThrow {
      inboundEventsListener.onMessage(
        raw(
          PrisonerReleasedEvent(
            ReleaseInformation(
              nomsNumber = "YD1234",
              prisonId = "TRN",
              reason = "TRANSFERRED",
            ),
          ),
        ),
      )
    }
  }

  @Test
  @Sql("classpath:integration-test-data/seed-prisoner-merge.sql")
  fun `should merge prison appointments and booking appointment history when prison merged`() {
    val firstBooking = videoBookingRepository.findById(-100).orElseThrow()
    val secondBooking = videoBookingRepository.findById(-200).orElseThrow()

    prisonAppointmentRepository.findByVideoBooking(firstBooking).single { it.prisonerNumber == "OLD123" }
    prisonAppointmentRepository.findByVideoBooking(secondBooking).single { it.prisonerNumber == "OLD123" }
    bookingHistoryAppointmentRepository.countByPrisonerNumber("OLD123") isEqualTo 2

    inboundEventsListener.onMessage(
      raw(
        PrisonerMergedEvent(
          MergeInformation(
            removedNomsNumber = "OLD123",
            nomsNumber = "NEW123",
          ),
        ),
      ),
    )

    prisonAppointmentRepository.findByVideoBooking(firstBooking).single { it.prisonerNumber == "NEW123" }
    prisonAppointmentRepository.findByVideoBooking(secondBooking).single { it.prisonerNumber == "NEW123" }
    bookingHistoryAppointmentRepository.countByPrisonerNumber("NEW123") isEqualTo 2
    verify(telemetryService).track(telemetryCaptor.capture())

    with(telemetryCaptor.firstValue) {
      properties() containsEntriesExactlyInAnyOrder mapOf(
        "previous_prisoner_number" to "OLD123",
        "new_prisoner_number" to "NEW123",
      )

      metrics() containsEntriesExactlyInAnyOrder mapOf("bookings_updated" to 2.0)
    }
  }

  @Test
  fun `should cancel a video court booking on receipt of a prisoner appointment cancelled event`() {
    videoBookingRepository.findAll() hasSize 0
    notificationRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", PENTONVILLE)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = PENTONVILLE,
      location = pentonvilleLocation,
      date = tomorrow(),
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, COURT_USER)

    videoBookingRepository.findById(bookingId).orElseThrow().statusCode isEqualTo StatusCode.ACTIVE

    inboundEventsListener.onMessage(
      raw(
        PrisonerVideoAppointmentCancelledEvent(
          personReference = PersonReference(listOf(Identifier("NOMS", "123456"))),
          additionalInformation = AppointmentScheduleInformation(
            scheduleEventId = 1,
            scheduledStartTime = tomorrow().atTime(12, 0),
            scheduledEndTime = null,
            scheduleEventSubType = "VLB",
            scheduleEventStatus = "",
            recordDeleted = true,
            agencyLocationId = PENTONVILLE,
          ),
        ),
      ),
    )

    val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow().also { it.statusCode isEqualTo StatusCode.CANCELLED }

    // There will be 5 notifications, 3 on back of create and 4 on back of cancellation
    val notifications = notificationRepository.findAll().also { it hasSize 7 }

    notifications.isPresent("court_user", NewCourtBookingUserEmail::class, persistedBooking)
    notifications.isPresent("g@g.com", NewCourtBookingPrisonCourtEmail::class, persistedBooking)
    notifications.isPresent("p@p.com", NewCourtBookingPrisonCourtEmail::class, persistedBooking)
    notifications.isPresent("b@b.com", CancelledCourtBookingCourtEmail::class, persistedBooking)
    notifications.isPresent("j@j.com", CancelledCourtBookingCourtEmail::class, persistedBooking)
    notifications.isPresent("g@g.com", CancelledCourtBookingPrisonCourtEmail::class, persistedBooking)
    notifications.isPresent("p@p.com", CancelledCourtBookingPrisonCourtEmail::class, persistedBooking)
  }

  @Test
  fun `should remove the pre-appointment for a video court booking on receipt of a prisoner appointment cancelled event`() {
    videoBookingRepository.findAll() hasSize 0
    notificationRepository.findAll() hasSize 0

    prisonSearchApi().stubGetPrisoner("123456", PENTONVILLE)

    val courtBookingRequest = courtBookingRequest(
      courtCode = DERBY_JUSTICE_CENTRE,
      prisonerNumber = "123456",
      prisonCode = PENTONVILLE,
      location = pentonvilleLocation,
      date = tomorrow(),
      startTime = LocalTime.of(12, 0),
      endTime = LocalTime.of(12, 30),
      comments = "integration test court booking comments",
      appointments = listOf(
        Appointment(
          type = AppointmentType.VLB_COURT_PRE,
          locationKey = pentonvilleLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(10, 45),
          endTime = LocalTime.of(11, 0),
        ),
        Appointment(
          type = AppointmentType.VLB_COURT_MAIN,
          locationKey = pentonvilleLocation.key,
          date = tomorrow(),
          startTime = LocalTime.of(11, 0),
          endTime = LocalTime.of(11, 30),
        ),
      ),
    )

    val bookingId = webTestClient.createBooking(courtBookingRequest, COURT_USER)
    val createdBooking = videoBookingRepository.findById(bookingId).orElseThrow().also { it.statusCode isEqualTo StatusCode.ACTIVE }

    prisonAppointmentRepository.findByVideoBooking(createdBooking) hasSize 2

    inboundEventsListener.onMessage(
      raw(
        PrisonerVideoAppointmentCancelledEvent(
          personReference = PersonReference(listOf(Identifier("NOMS", "123456"))),
          additionalInformation = AppointmentScheduleInformation(
            scheduleEventId = 1,
            scheduledStartTime = tomorrow().atTime(10, 45),
            scheduledEndTime = null,
            scheduleEventSubType = "VLB",
            scheduleEventStatus = "",
            recordDeleted = true,
            agencyLocationId = PENTONVILLE,
          ),
        ),
      ),
    )

    val updatedBooking = videoBookingRepository.findById(bookingId).orElseThrow()

    with(updatedBooking) {
      statusCode isEqualTo StatusCode.ACTIVE
      amendedBy isEqualTo SERVICE_USER.username
      amendedTime isCloseTo LocalDateTime.now()
    }

    prisonAppointmentRepository.findByVideoBooking(updatedBooking).single().appointmentType isEqualTo "VLB_COURT_MAIN"

    // There will be only be 3 notifications for the initial creation, there is no actual cancellation in this case.
    val notifications = notificationRepository.findAll().also { it hasSize 3 }

    notifications.isPresent("court_user", NewCourtBookingUserEmail::class, updatedBooking)
    notifications.isPresent("g@g.com", NewCourtBookingPrisonCourtEmail::class, updatedBooking)
    notifications.isPresent("p@p.com", NewCourtBookingPrisonCourtEmail::class, updatedBooking)
  }

  @Test
  fun `should cancel a video probation booking on receipt of a prisoner appointment cancelled event`() {
    videoBookingRepository.findAll() hasSize 0
    notificationRepository.findAll() hasSize 0

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

    videoBookingRepository.findById(bookingId).orElseThrow().statusCode isEqualTo StatusCode.ACTIVE

    inboundEventsListener.onMessage(
      raw(
        PrisonerVideoAppointmentCancelledEvent(
          personReference = PersonReference(listOf(Identifier("NOMS", "123456"))),
          additionalInformation = AppointmentScheduleInformation(
            scheduleEventId = 1,
            scheduledStartTime = tomorrow().atTime(9, 0),
            scheduledEndTime = null,
            scheduleEventSubType = "VLB",
            scheduleEventStatus = "",
            recordDeleted = true,
            agencyLocationId = BIRMINGHAM,
          ),
        ),
      ),
    )

    val persistedBooking = videoBookingRepository.findById(bookingId).orElseThrow().also { it.statusCode isEqualTo StatusCode.CANCELLED }

    // There will be 5 notifications, 2 on back of create and 3 on back of cancellation
    val notifications = notificationRepository.findAll().also { it hasSize 5 }

    notifications.isPresent("probation.user@probation.com", NewProbationBookingUserEmail::class, persistedBooking)
    notifications.isPresent("a@a.com", NewProbationBookingPrisonProbationEmail::class, persistedBooking)
    notifications.isPresent("t@t.com", CancelledProbationBookingProbationEmail::class, persistedBooking)
    notifications.isPresent("m@m.com", CancelledProbationBookingProbationEmail::class, persistedBooking)
    notifications.isPresent("a@a.com", CancelledProbationBookingPrisonProbationEmail::class, persistedBooking)
  }

  private fun <T : Email> Collection<Notification>.isPresent(email: String, template: KClass<T>, booking: VideoBooking? = null) {
    val match = singleOrNull { it.email == email && it.templateName == template.simpleName && it.videoBooking == booking }

    requireNotNull(match) {
      "Notification not present: email '$email', template '${template.simpleName}'"
    }
  }
}

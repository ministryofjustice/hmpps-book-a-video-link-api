package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.facade

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DELIUS_PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.SERVICE_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendCourtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendProbationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isInstanceOf
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withProbationPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.yesterday
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ChangeTrackingService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ChangeType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.VideoBookingServiceDelegate
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.BookingDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.availability.AvailabilityService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.CourtBookingAmendedTelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.CourtBookingCancelledTelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.CourtBookingCreatedTelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.ProbationBookingAmendedTelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.ProbationBookingCancelledTelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.ProbationBookingCreatedTelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.TelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.TelemetryService
import java.time.LocalDate
import java.time.LocalDateTime.now
import java.time.LocalTime

class BookingFacadeTest {
  private val videoBookingServiceDelegate: VideoBookingServiceDelegate = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val prisonerSearchClient: PrisonerSearchClient = mock()
  private val telemetryService: TelemetryService = mock()
  private val telemetryCaptor = argumentCaptor<TelemetryEvent>()
  private val availabilityService: AvailabilityService = mock()
  private val changeTrackingService: ChangeTrackingService = mock()
  private val emailFacade: EmailFacade = mock()
  private val bookingDetailsCaptor = argumentCaptor<BookingDetails>()
  private val userCaptor = argumentCaptor<User>()
  private val changeTypeCaptor = argumentCaptor<ChangeType>()

  private val facade = BookingFacade(
    videoBookingServiceDelegate,
    outboundEventsService,
    prisonerSearchClient,
    telemetryService,
    availabilityService,
    changeTrackingService,
    emailFacade,
  )
  private val courtBooking = courtBooking(notesForStaff = "court notes for staff")
    .addAppointment(
      prison = prison(prisonCode = WANDSWORTH),
      prisonerNumber = "123456",
      appointmentType = "VLB_COURT_MAIN",
      date = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      locationId = wandsworthLocation.id,
    )
  private val courtBookingAtDisabledCourt = courtBooking(court = court(enabled = false))
    .addAppointment(
      prison = prison(prisonCode = WANDSWORTH),
      prisonerNumber = "123456",
      appointmentType = "VLB_COURT_MAIN",
      date = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      locationId = wandsworthLocation.id,
    )
  private val courtBookingInThePast = courtBooking()
    .addAppointment(
      prison = prison(prisonCode = WANDSWORTH),
      prisonerNumber = "123456",
      appointmentType = "VLB_COURT_MAIN",
      date = LocalDate.now().minusDays(1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      locationId = wandsworthLocation.id,
    )
  private val courtBookingCreatedByPrison = courtBooking(createdByPrison = true, notesForStaff = "court notes for staff", notesForPrisoners = "court notes for prisoners")
    .addAppointment(
      prison = prison(prisonCode = WANDSWORTH),
      prisonerNumber = "123456",
      appointmentType = "VLB_COURT_MAIN",
      date = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      locationId = wandsworthLocation.id,
    )

  private val probationBookingAtBirminghamPrison = probationBooking(notesForStaff = "probation notes for staff")
    .addAppointment(
      prison = prison(prisonCode = BIRMINGHAM),
      prisonerNumber = "654321",
      appointmentType = AppointmentType.VLB_PROBATION.name,
      locationId = birminghamLocation.id,
      date = tomorrow(),
      startTime = LocalTime.MIDNIGHT,
      endTime = LocalTime.MIDNIGHT.plusHours(1),
    )

  private val probationBookingAtBirminghamPrisonCreatedByPrison = probationBooking(createdBy = PRISON_USER_BIRMINGHAM, notesForStaff = "probation notes for staff", notesForPrisoners = "probation notes for staff prisoner")
    .addAppointment(
      prison = prison(prisonCode = BIRMINGHAM),
      prisonerNumber = "654321",
      appointmentType = AppointmentType.VLB_PROBATION.name,
      locationId = birminghamLocation.id,
      date = tomorrow(),
      startTime = LocalTime.MIDNIGHT,
      endTime = LocalTime.MIDNIGHT.plusHours(1),
    )

  @BeforeEach
  fun before() {
    whenever(availabilityService.isAvailable(any<CreateVideoBookingRequest>())) doReturn true
    whenever(availabilityService.isAvailable(anyLong(), any())) doReturn true
    whenever(changeTrackingService.determineChangeType(any(), any(), any())) doReturn ChangeType.GLOBAL
  }

  @Nested
  @DisplayName("Create bookings")
  inner class CreateBooking {
    @Test
    fun `should send events and emails on creation of court booking by court user`() {
      val bookingRequest = courtBookingRequest(prisonCode = WANDSWORTH, prisonerNumber = courtBooking.prisoner())

      videoBookingServiceDelegate.stub {
        on { create(any(), any()) } doReturn Pair(courtBooking, prisoner(prisonerNumber = courtBooking.prisoner(), prisonCode = WANDSWORTH))
        on { getVideoBookingById(anyLong(), any()) } doReturn courtBooking.toModel(setOf(wandsworthLocation.toModel()))
      }

      facade.create(bookingRequest, COURT_USER)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailFacade, telemetryService) {
        verify(videoBookingServiceDelegate).create(bookingRequest, COURT_USER)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CREATED, courtBooking.videoBookingId)
        verify(videoBookingServiceDelegate).getVideoBookingById(anyLong(), eq(COURT_USER))
        verify(emailFacade).sendEmails(bookingDetailsCaptor.capture(), userCaptor.capture(), changeTypeCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      bookingDetailsCaptor.singleValue isInstanceOf BookingDetails.Create::class.java
      userCaptor.singleValue isEqualTo COURT_USER
      changeTypeCaptor.singleValue isEqualTo ChangeType.GLOBAL

      with(telemetryCaptor.singleValue as CourtBookingCreatedTelemetryEvent) {
        properties()["created_by"] isEqualTo "court"
      }
    }

    @Test
    fun `should fail to create court booking if location not available`() {
      val createdRequest = courtBookingRequest()

      whenever(availabilityService.isAvailable(createdRequest)) doReturn false

      val error = assertThrows<IllegalArgumentException> { facade.create(createdRequest, COURT_USER) }
      error.message isEqualTo "Unable to create court booking, booking overlaps with an existing appointment."
    }

    @Test
    fun `should send events and emails on creation of court booking by a prison user`() {
      val bookingRequest = courtBookingRequest(prisonCode = WANDSWORTH, prisonerNumber = courtBookingCreatedByPrison.prisoner())

      videoBookingServiceDelegate.stub {
        on { create(any(), any()) } doReturn Pair(courtBookingCreatedByPrison, prisoner(prisonerNumber = courtBookingCreatedByPrison.prisoner(), prisonCode = WANDSWORTH))
        on { getVideoBookingById(anyLong(), any()) } doReturn courtBookingCreatedByPrison.toModel(setOf(wandsworthLocation.toModel()))
      }

      facade.create(bookingRequest, PRISON_USER_BIRMINGHAM)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailFacade, telemetryService) {
        verify(videoBookingServiceDelegate).create(bookingRequest, PRISON_USER_BIRMINGHAM)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CREATED, courtBookingCreatedByPrison.videoBookingId)
        verify(emailFacade).sendEmails(bookingDetailsCaptor.capture(), userCaptor.capture(), changeTypeCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      bookingDetailsCaptor.singleValue isInstanceOf BookingDetails.Create::class.java
      userCaptor.singleValue isEqualTo PRISON_USER_BIRMINGHAM
      changeTypeCaptor.singleValue isEqualTo ChangeType.GLOBAL

      with(telemetryCaptor.singleValue as CourtBookingCreatedTelemetryEvent) {
        properties()["created_by"] isEqualTo "prison"
      }
    }

    @Test
    fun `should send events and emails on creation of probation booking by probation user`() {
      val prisoner = Prisoner(probationBookingAtBirminghamPrison.prisoner(), BIRMINGHAM, "Bob", "Builder", yesterday())

      whenever(prisonerSearchClient.getPrisoner(prisoner.prisonerNumber)) doReturn prisoner

      val request = probationBookingRequest(
        prisonCode = prisoner.prisonId!!,
        prisonerNumber = prisoner.prisonerNumber,
        appointmentDate = tomorrow(),
        startTime = LocalTime.MIDNIGHT,
        endTime = LocalTime.MIDNIGHT.plusHours(1),
      )

      videoBookingServiceDelegate.stub {
        on { create(any(), any()) } doReturn Pair(probationBookingAtBirminghamPrison, prisoner(prisoner))
        on { getVideoBookingById(anyLong(), any()) } doReturn probationBookingAtBirminghamPrison.toModel(setOf(birminghamLocation.toModel()))
      }

      facade.create(request, PROBATION_USER)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailFacade, telemetryService) {
        verify(videoBookingServiceDelegate).create(request, PROBATION_USER)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CREATED, probationBookingAtBirminghamPrison.videoBookingId)
        verify(emailFacade).sendEmails(bookingDetailsCaptor.capture(), userCaptor.capture(), changeTypeCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      bookingDetailsCaptor.singleValue isInstanceOf BookingDetails.Create::class.java
      userCaptor.singleValue isEqualTo PROBATION_USER
      changeTypeCaptor.singleValue isEqualTo ChangeType.GLOBAL

      with(telemetryCaptor.singleValue as ProbationBookingCreatedTelemetryEvent) {
        properties()["created_by"] isEqualTo "probation"
      }
    }

    @Test
    fun `should send events and emails on creation of probation booking by prison user`() {
      val prisoner = Prisoner(probationBookingAtBirminghamPrisonCreatedByPrison.prisoner(), BIRMINGHAM, "Bob", "Builder", yesterday())

      whenever(prisonerSearchClient.getPrisoner(prisoner.prisonerNumber)) doReturn prisoner

      val request = probationBookingRequest(
        prisonCode = prisoner.prisonId!!,
        prisonerNumber = prisoner.prisonerNumber,
        appointmentDate = tomorrow(),
        startTime = LocalTime.MIDNIGHT,
        endTime = LocalTime.MIDNIGHT.plusHours(1),
      )

      videoBookingServiceDelegate.stub {
        on { create(any(), any()) } doReturn Pair(probationBookingAtBirminghamPrisonCreatedByPrison, prisoner(prisoner))
        on { getVideoBookingById(anyLong(), any()) } doReturn probationBookingAtBirminghamPrisonCreatedByPrison.toModel(setOf(birminghamLocation.toModel()))
      }

      facade.create(request, PRISON_USER_BIRMINGHAM)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailFacade, telemetryService) {
        verify(videoBookingServiceDelegate).create(request, PRISON_USER_BIRMINGHAM)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CREATED, probationBookingAtBirminghamPrisonCreatedByPrison.videoBookingId)
        verify(emailFacade).sendEmails(bookingDetailsCaptor.capture(), userCaptor.capture(), changeTypeCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      bookingDetailsCaptor.singleValue isInstanceOf BookingDetails.Create::class.java
      userCaptor.singleValue isEqualTo PRISON_USER_BIRMINGHAM
      changeTypeCaptor.singleValue isEqualTo ChangeType.GLOBAL

      with(telemetryCaptor.singleValue as ProbationBookingCreatedTelemetryEvent) {
        properties()["created_by"] isEqualTo "prison"
      }
    }

    @Test
    fun `should fail to create probation booking if location not available`() {
      val createdRequest = probationBookingRequest()

      whenever(availabilityService.isAvailable(createdRequest)) doReturn false

      val error = assertThrows<IllegalArgumentException> { facade.create(createdRequest, PROBATION_USER) }
      error.message isEqualTo "Unable to create probation booking, booking overlaps with an existing appointment."
    }
  }

  @Nested
  @DisplayName("Cancel bookings")
  inner class CancelBooking {
    @Test
    fun `should send events and emails on cancellation of court booking by court user`() {
      val prisoner = Prisoner(courtBooking.prisoner(), WANDSWORTH, "Bob", "Builder", yesterday())

      videoBookingServiceDelegate.stub {
        on { cancel(courtBooking.videoBookingId, COURT_USER) } doReturn courtBooking.cancel(COURT_USER)
        on { getVideoBookingById(courtBooking.videoBookingId, COURT_USER) } doReturn courtBooking.toModel(setOf(wandsworthLocation.toModel()))
      }

      whenever(prisonerSearchClient.getPrisoner(courtBooking.prisoner())) doReturn prisoner

      facade.cancel(courtBooking.videoBookingId, COURT_USER)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailFacade, telemetryService) {
        verify(videoBookingServiceDelegate).cancel(courtBooking.videoBookingId, COURT_USER)
        verify(emailFacade).sendEmails(bookingDetailsCaptor.capture(), userCaptor.capture(), changeTypeCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, courtBooking.videoBookingId)
      }

      bookingDetailsCaptor.singleValue isInstanceOf BookingDetails.Cancelled::class.java
      userCaptor.singleValue isEqualTo COURT_USER
      changeTypeCaptor.singleValue isEqualTo ChangeType.GLOBAL

      with(telemetryCaptor.singleValue as CourtBookingCancelledTelemetryEvent) {
        properties()["cancelled_by"] isEqualTo "court"
      }
    }

    @Test
    fun `should send events and emails on cancellation of court booking by prison user`() {
      val prisoner = Prisoner(courtBooking.prisoner(), WANDSWORTH, "Bob", "Builder", yesterday())

      videoBookingServiceDelegate.stub {
        on { cancel(courtBooking.videoBookingId, PRISON_USER_BIRMINGHAM) } doReturn courtBooking.cancel(PRISON_USER_BIRMINGHAM)
        on { getVideoBookingById(courtBooking.videoBookingId, PRISON_USER_BIRMINGHAM) } doReturn courtBooking.toModel(setOf(wandsworthLocation.toModel()))
      }

      whenever(prisonerSearchClient.getPrisoner(courtBooking.prisoner())) doReturn prisoner

      facade.cancel(courtBooking.videoBookingId, PRISON_USER_BIRMINGHAM)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailFacade, telemetryService) {
        verify(videoBookingServiceDelegate).cancel(courtBooking.videoBookingId, PRISON_USER_BIRMINGHAM)
        verify(emailFacade).sendEmails(bookingDetailsCaptor.capture(), userCaptor.capture(), changeTypeCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, courtBooking.videoBookingId)
      }

      bookingDetailsCaptor.singleValue isInstanceOf BookingDetails.Cancelled::class.java
      userCaptor.singleValue isEqualTo PRISON_USER_BIRMINGHAM
      changeTypeCaptor.singleValue isEqualTo ChangeType.GLOBAL

      with(telemetryCaptor.singleValue as CourtBookingCancelledTelemetryEvent) {
        properties()["cancelled_by"] isEqualTo "prison"
      }
    }

    @Test
    fun `should send emails but no events on cancellation of a court booking by service user`() {
      val prisoner = Prisoner(courtBooking.prisoner(), WANDSWORTH, "Bob", "Builder", yesterday())

      videoBookingServiceDelegate.stub {
        on { cancel(courtBooking.videoBookingId, SERVICE_USER) } doReturn courtBooking.cancel(SERVICE_USER)
        on { getVideoBookingById(courtBooking.videoBookingId, SERVICE_USER) } doReturn courtBooking.toModel(setOf(wandsworthLocation.toModel()))
      }

      whenever(prisonerSearchClient.getPrisoner(courtBooking.prisoner())) doReturn prisoner

      facade.cancel(courtBooking.videoBookingId, SERVICE_USER)

      inOrder(videoBookingServiceDelegate, emailFacade, telemetryService) {
        verify(videoBookingServiceDelegate).cancel(courtBooking.videoBookingId, SERVICE_USER)
        verify(emailFacade).sendEmails(bookingDetailsCaptor.capture(), userCaptor.capture(), changeTypeCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      bookingDetailsCaptor.singleValue isInstanceOf BookingDetails.Cancelled::class.java
      userCaptor.singleValue isEqualTo SERVICE_USER
      changeTypeCaptor.singleValue isEqualTo ChangeType.GLOBAL

      verifyNoInteractions(outboundEventsService)

      with(telemetryCaptor.singleValue as CourtBookingCancelledTelemetryEvent) {
        properties()["cancelled_by"] isEqualTo "prison"
      }
    }

    @Test
    fun `should send events and emails on cancellation of probation booking by probation user`() {
      val prisoner = Prisoner(probationBookingAtBirminghamPrison.prisoner(), BIRMINGHAM, "Bob", "Builder", yesterday())

      videoBookingServiceDelegate.stub {
        on { cancel(probationBookingAtBirminghamPrison.videoBookingId, PROBATION_USER) } doReturn probationBookingAtBirminghamPrison.cancel(PROBATION_USER)
        on { getVideoBookingById(probationBookingAtBirminghamPrison.videoBookingId, PROBATION_USER) } doReturn probationBookingAtBirminghamPrison.toModel(setOf(birminghamLocation.toModel()))
      }

      whenever(prisonerSearchClient.getPrisoner(prisoner.prisonerNumber)) doReturn prisoner

      facade.cancel(probationBookingAtBirminghamPrison.videoBookingId, PROBATION_USER)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailFacade, telemetryService) {
        verify(videoBookingServiceDelegate).cancel(probationBookingAtBirminghamPrison.videoBookingId, PROBATION_USER)
        verify(emailFacade).sendEmails(bookingDetailsCaptor.capture(), userCaptor.capture(), changeTypeCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, probationBookingAtBirminghamPrison.videoBookingId)
      }

      bookingDetailsCaptor.singleValue isInstanceOf BookingDetails.Cancelled::class.java
      userCaptor.singleValue isEqualTo PROBATION_USER
      changeTypeCaptor.singleValue isEqualTo ChangeType.GLOBAL

      with(telemetryCaptor.singleValue as ProbationBookingCancelledTelemetryEvent) {
        properties()["cancelled_by"] isEqualTo "probation"
      }
    }

    @Test
    fun `should send events and emails on cancellation of probation booking by Delius probation user`() {
      val prisoner = Prisoner(probationBookingAtBirminghamPrison.prisoner(), BIRMINGHAM, "Bob", "Builder", yesterday())

      videoBookingServiceDelegate.stub {
        on { cancel(probationBookingAtBirminghamPrison.videoBookingId, DELIUS_PROBATION_USER) } doReturn probationBookingAtBirminghamPrison.cancel(DELIUS_PROBATION_USER)
        on { getVideoBookingById(probationBookingAtBirminghamPrison.videoBookingId, DELIUS_PROBATION_USER) } doReturn probationBookingAtBirminghamPrison.toModel(setOf(birminghamLocation.toModel()))
      }

      whenever(prisonerSearchClient.getPrisoner(prisoner.prisonerNumber)) doReturn prisoner

      facade.cancel(probationBookingAtBirminghamPrison.videoBookingId, DELIUS_PROBATION_USER)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailFacade, telemetryService) {
        verify(videoBookingServiceDelegate).cancel(probationBookingAtBirminghamPrison.videoBookingId, DELIUS_PROBATION_USER)
        verify(emailFacade).sendEmails(bookingDetailsCaptor.capture(), userCaptor.capture(), changeTypeCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, probationBookingAtBirminghamPrison.videoBookingId)
      }

      bookingDetailsCaptor.singleValue isInstanceOf BookingDetails.Cancelled::class.java
      userCaptor.singleValue isEqualTo DELIUS_PROBATION_USER
      changeTypeCaptor.singleValue isEqualTo ChangeType.GLOBAL

      with(telemetryCaptor.singleValue as ProbationBookingCancelledTelemetryEvent) {
        properties()["cancelled_by"] isEqualTo "probation"
      }
    }

    @Test
    fun `should send events and emails on cancellation of probation booking by prison user`() {
      val prisoner = Prisoner(probationBookingAtBirminghamPrison.prisoner(), BIRMINGHAM, "Bob", "Builder", yesterday())

      videoBookingServiceDelegate.stub {
        on { cancel(probationBookingAtBirminghamPrison.videoBookingId, PRISON_USER_BIRMINGHAM) } doReturn probationBookingAtBirminghamPrison.cancel(PRISON_USER_BIRMINGHAM)
        on { getVideoBookingById(probationBookingAtBirminghamPrison.videoBookingId, PRISON_USER_BIRMINGHAM) } doReturn probationBookingAtBirminghamPrison.toModel(setOf(birminghamLocation.toModel()))
      }

      whenever(prisonerSearchClient.getPrisoner(prisoner.prisonerNumber)) doReturn prisoner

      facade.cancel(probationBookingAtBirminghamPrison.videoBookingId, PRISON_USER_BIRMINGHAM)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailFacade, telemetryService) {
        verify(videoBookingServiceDelegate).cancel(probationBookingAtBirminghamPrison.videoBookingId, PRISON_USER_BIRMINGHAM)
        verify(emailFacade).sendEmails(bookingDetailsCaptor.capture(), userCaptor.capture(), changeTypeCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, probationBookingAtBirminghamPrison.videoBookingId)
      }

      bookingDetailsCaptor.singleValue isInstanceOf BookingDetails.Cancelled::class.java
      userCaptor.singleValue isEqualTo PRISON_USER_BIRMINGHAM
      changeTypeCaptor.singleValue isEqualTo ChangeType.GLOBAL

      with(telemetryCaptor.singleValue as ProbationBookingCancelledTelemetryEvent) {
        properties()["cancelled_by"] isEqualTo "prison"
      }
    }

    @Test
    fun `should send emails but no events on cancellation of a probation booking by service user`() {
      val prisoner = Prisoner(probationBookingAtBirminghamPrison.prisoner(), BIRMINGHAM, "Bob", "Builder", yesterday())

      videoBookingServiceDelegate.stub {
        on { cancel(probationBookingAtBirminghamPrison.videoBookingId, SERVICE_USER) } doReturn probationBookingAtBirminghamPrison.cancel(SERVICE_USER)
        on { getVideoBookingById(probationBookingAtBirminghamPrison.videoBookingId, SERVICE_USER) } doReturn probationBookingAtBirminghamPrison.toModel(setOf(birminghamLocation.toModel()))
      }

      whenever(prisonerSearchClient.getPrisoner(prisoner.prisonerNumber)) doReturn prisoner

      facade.cancel(probationBookingAtBirminghamPrison.videoBookingId, SERVICE_USER)

      inOrder(videoBookingServiceDelegate, emailFacade, telemetryService) {
        verify(videoBookingServiceDelegate).cancel(probationBookingAtBirminghamPrison.videoBookingId, SERVICE_USER)
        verify(emailFacade).sendEmails(bookingDetailsCaptor.capture(), userCaptor.capture(), changeTypeCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      bookingDetailsCaptor.singleValue isInstanceOf BookingDetails.Cancelled::class.java
      userCaptor.singleValue isEqualTo SERVICE_USER
      changeTypeCaptor.singleValue isEqualTo ChangeType.GLOBAL

      verifyNoInteractions(outboundEventsService)

      with(telemetryCaptor.singleValue as ProbationBookingCancelledTelemetryEvent) {
        properties()["cancelled_by"] isEqualTo "prison"
      }
    }
  }

  @Nested
  @DisplayName("Amend bookings")
  inner class AmendBooking {
    @Test
    fun `should send events and emails on amendment of court booking by court user`() {
      val bookingRequest = amendCourtBookingRequest(prisonCode = WANDSWORTH, prisonerNumber = "123456")

      whenever(
        videoBookingServiceDelegate.amend(
          courtBooking.videoBookingId,
          bookingRequest,
          COURT_USER,
        ),
      ) doReturn Pair(
        courtBooking.apply {
          amendedTime = now()
          amendedBy = COURT_USER.username
        },
        prisoner(prisonerNumber = "123456", prisonCode = WANDSWORTH),
      )
      whenever { videoBookingServiceDelegate.getVideoBookingById(courtBooking.videoBookingId, COURT_USER) } doReturn courtBooking.toModel(setOf(wandsworthLocation.toModel()))

      facade.amend(courtBooking.videoBookingId, bookingRequest, COURT_USER)

      inOrder(videoBookingServiceDelegate, emailFacade, telemetryService) {
        verify(videoBookingServiceDelegate).amend(courtBooking.videoBookingId, bookingRequest, COURT_USER)
        verify(emailFacade).sendEmails(bookingDetailsCaptor.capture(), userCaptor.capture(), changeTypeCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      bookingDetailsCaptor.singleValue isInstanceOf BookingDetails.Amended::class.java
      userCaptor.singleValue isEqualTo COURT_USER
      changeTypeCaptor.singleValue isEqualTo ChangeType.GLOBAL

      with(telemetryCaptor.singleValue as CourtBookingAmendedTelemetryEvent) {
        properties()["amended_by"] isEqualTo "court"
      }
    }

    @Test
    fun `should fail to amend court booking if location not available`() {
      val amendRequest = amendCourtBookingRequest()

      whenever(availabilityService.isAvailable(1, amendRequest)) doReturn false

      val error = assertThrows<IllegalArgumentException> { facade.amend(1, amendRequest, COURT_USER) }
      error.message isEqualTo "Unable to amend court booking, booking overlaps with an existing appointment."
    }

    @Test
    fun `should send events and emails on amendment of court booking by prison user`() {
      val bookingRequest = amendCourtBookingRequest(prisonCode = WANDSWORTH, prisonerNumber = "123456")

      whenever(videoBookingServiceDelegate.amend(courtBooking.videoBookingId, bookingRequest, PRISON_USER_BIRMINGHAM)) doReturn
        Pair(
          courtBooking.apply {
            amendedTime = now()
            amendedBy = PRISON_USER_BIRMINGHAM.username
          },
          prisoner(prisonerNumber = "123456", prisonCode = WANDSWORTH),
        )
      whenever { videoBookingServiceDelegate.getVideoBookingById(courtBooking.videoBookingId, PRISON_USER_BIRMINGHAM) } doReturn courtBooking.toModel(setOf(wandsworthLocation.toModel()))

      facade.amend(courtBooking.videoBookingId, bookingRequest, PRISON_USER_BIRMINGHAM)

      inOrder(videoBookingServiceDelegate, emailFacade, telemetryService) {
        verify(videoBookingServiceDelegate).amend(courtBooking.videoBookingId, bookingRequest, PRISON_USER_BIRMINGHAM)
        verify(emailFacade).sendEmails(bookingDetailsCaptor.capture(), userCaptor.capture(), changeTypeCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      bookingDetailsCaptor.singleValue isInstanceOf BookingDetails.Amended::class.java
      userCaptor.singleValue isEqualTo PRISON_USER_BIRMINGHAM
      changeTypeCaptor.singleValue isEqualTo ChangeType.GLOBAL

      with(telemetryCaptor.singleValue as CourtBookingAmendedTelemetryEvent) {
        properties()["amended_by"] isEqualTo "prison"
      }
    }

    @Test
    fun `should send events and reduced emails on amendment of court booking by prison user`() {
      val amendRequest = amendCourtBookingRequest(prisonCode = WANDSWORTH, prisonerNumber = "123456")

      whenever(videoBookingServiceDelegate.amend(courtBooking.videoBookingId, amendRequest, PRISON_USER_BIRMINGHAM)) doReturn
        Pair(
          courtBooking.apply {
            amendedTime = now()
            amendedBy = PRISON_USER_BIRMINGHAM.username
          },
          prisoner(prisonerNumber = "123456", prisonCode = WANDSWORTH),
        )
      whenever { videoBookingServiceDelegate.getVideoBookingById(courtBooking.videoBookingId, PRISON_USER_BIRMINGHAM) } doReturn courtBooking.toModel(setOf(wandsworthLocation.toModel()))
      whenever(changeTrackingService.determineChangeType(courtBooking.videoBookingId, amendRequest, PRISON_USER_BIRMINGHAM)) doReturn ChangeType.PRISON

      facade.amend(courtBooking.videoBookingId, amendRequest, PRISON_USER_BIRMINGHAM)

      inOrder(videoBookingServiceDelegate, emailFacade, telemetryService) {
        verify(videoBookingServiceDelegate).amend(courtBooking.videoBookingId, amendRequest, PRISON_USER_BIRMINGHAM)
        verify(emailFacade).sendEmails(bookingDetailsCaptor.capture(), userCaptor.capture(), changeTypeCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      bookingDetailsCaptor.singleValue isInstanceOf BookingDetails.Amended::class.java
      userCaptor.singleValue isEqualTo PRISON_USER_BIRMINGHAM
      changeTypeCaptor.singleValue isEqualTo ChangeType.PRISON

      with(telemetryCaptor.singleValue as CourtBookingAmendedTelemetryEvent) {
        properties()["amended_by"] isEqualTo "prison"
      }
    }

    @Test
    fun `should send events and emails on amendment of probation booking by probation user`() {
      val prisoner = Prisoner(probationBookingAtBirminghamPrison.prisoner(), BIRMINGHAM, "Bob", "Builder", yesterday())

      whenever(prisonerSearchClient.getPrisoner(prisoner.prisonerNumber)) doReturn prisoner

      val amendRequest = amendProbationBookingRequest(prisonCode = prisoner.prisonId!!, prisonerNumber = prisoner.prisonerNumber, appointmentDate = tomorrow())

      whenever(videoBookingServiceDelegate.amend(probationBookingAtBirminghamPrison.videoBookingId, amendRequest, PROBATION_USER)) doReturn Pair(
        probationBookingAtBirminghamPrison.apply {
          amendedTime = now()
          amendedBy = PROBATION_USER.username
        },
        prisoner(prisonerNumber = prisoner.prisonerNumber, prisonCode = prisoner.prisonId),
      )
      whenever { videoBookingServiceDelegate.getVideoBookingById(probationBookingAtBirminghamPrison.videoBookingId, PROBATION_USER) } doReturn probationBookingAtBirminghamPrison.toModel(setOf(birminghamLocation.toModel()))

      facade.amend(probationBookingAtBirminghamPrison.videoBookingId, amendRequest, PROBATION_USER)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailFacade, telemetryService) {
        verify(videoBookingServiceDelegate).amend(probationBookingAtBirminghamPrison.videoBookingId, amendRequest, PROBATION_USER)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_AMENDED, probationBookingAtBirminghamPrison.videoBookingId)
        verify(emailFacade).sendEmails(bookingDetailsCaptor.capture(), userCaptor.capture(), changeTypeCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      bookingDetailsCaptor.singleValue isInstanceOf BookingDetails.Amended::class.java
      userCaptor.singleValue isEqualTo PROBATION_USER
      changeTypeCaptor.singleValue isEqualTo ChangeType.GLOBAL

      with(telemetryCaptor.singleValue as ProbationBookingAmendedTelemetryEvent) {
        properties()["amended_by"] isEqualTo "probation"
      }
    }

    @Test
    fun `should send events and emails on amendment of probation booking by prison user`() {
      val prisoner = Prisoner(probationBookingAtBirminghamPrison.prisoner(), BIRMINGHAM, "Bob", "Builder", yesterday())

      whenever(prisonerSearchClient.getPrisoner(prisoner.prisonerNumber)) doReturn prisoner

      val amendRequest = amendProbationBookingRequest(prisonCode = prisoner.prisonId!!, prisonerNumber = prisoner.prisonerNumber, appointmentDate = tomorrow())

      whenever(videoBookingServiceDelegate.amend(probationBookingAtBirminghamPrison.videoBookingId, amendRequest, PRISON_USER_BIRMINGHAM)) doReturn Pair(
        probationBookingAtBirminghamPrison.apply {
          amendedTime = now()
          amendedBy = PRISON_USER_BIRMINGHAM.username
        },
        prisoner(prisonerNumber = prisoner.prisonerNumber, prisonCode = prisoner.prisonId),
      )
      whenever { videoBookingServiceDelegate.getVideoBookingById(probationBookingAtBirminghamPrison.videoBookingId, PRISON_USER_BIRMINGHAM) } doReturn probationBookingAtBirminghamPrison.toModel(setOf(birminghamLocation.toModel()))

      facade.amend(probationBookingAtBirminghamPrison.videoBookingId, amendRequest, PRISON_USER_BIRMINGHAM)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailFacade, telemetryService) {
        verify(videoBookingServiceDelegate).amend(probationBookingAtBirminghamPrison.videoBookingId, amendRequest, PRISON_USER_BIRMINGHAM)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_AMENDED, probationBookingAtBirminghamPrison.videoBookingId)
        verify(emailFacade).sendEmails(bookingDetailsCaptor.capture(), userCaptor.capture(), changeTypeCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      bookingDetailsCaptor.singleValue isInstanceOf BookingDetails.Amended::class.java
      userCaptor.singleValue isEqualTo PRISON_USER_BIRMINGHAM
      changeTypeCaptor.singleValue isEqualTo ChangeType.GLOBAL

      with(telemetryCaptor.singleValue as ProbationBookingAmendedTelemetryEvent) {
        properties()["amended_by"] isEqualTo "prison"
      }
    }

    @Test
    fun `should fail to amend probation booking if location not available`() {
      val amendRequest = amendProbationBookingRequest()

      whenever(availabilityService.isAvailable(1, amendRequest)) doReturn false

      val error = assertThrows<IllegalArgumentException> { facade.amend(1, amendRequest, PROBATION_USER) }
      error.message isEqualTo "Unable to amend probation booking, booking overlaps with an existing appointment."
    }

    @Test
    fun `should not send emails when no actual changes`() {
      val prisoner = Prisoner(probationBookingAtBirminghamPrison.prisoner(), BIRMINGHAM, "Bob", "Builder", yesterday())

      whenever(prisonerSearchClient.getPrisoner(prisoner.prisonerNumber)) doReturn prisoner

      val amendRequest = amendProbationBookingRequest(prisonCode = prisoner.prisonId!!, prisonerNumber = prisoner.prisonerNumber, appointmentDate = tomorrow())

      whenever(videoBookingServiceDelegate.amend(1, amendRequest, PRISON_USER_BIRMINGHAM)) doReturn Pair(
        probationBookingAtBirminghamPrison.apply {
          amendedTime = now()
          amendedBy = PRISON_USER_BIRMINGHAM.username
        },
        prisoner(prisonerNumber = prisoner.prisonerNumber, prisonCode = prisoner.prisonId),
      )

      whenever(changeTrackingService.determineChangeType(1, amendRequest, PRISON_USER_BIRMINGHAM)) doReturn ChangeType.NONE

      facade.amend(1, amendRequest, PRISON_USER_BIRMINGHAM)

      verify(changeTrackingService).determineChangeType(1, amendRequest, PRISON_USER_BIRMINGHAM)
      verify(videoBookingServiceDelegate).amend(1, amendRequest, PRISON_USER_BIRMINGHAM)
      verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_AMENDED, 1)
      verifyNoInteractions(emailFacade)
      verify(telemetryService).track(any())
    }

    @Test
    fun `should send events and reduced emails on amendment of probation booking by prison user`() {
      val prisoner = Prisoner(probationBookingAtBirminghamPrison.prisoner(), BIRMINGHAM, "Bob", "Builder", yesterday())

      whenever(prisonerSearchClient.getPrisoner(prisoner.prisonerNumber)) doReturn prisoner

      val amendRequest = amendProbationBookingRequest(prisonCode = prisoner.prisonId!!, prisonerNumber = prisoner.prisonerNumber, appointmentDate = tomorrow())

      whenever(videoBookingServiceDelegate.amend(probationBookingAtBirminghamPrison.videoBookingId, amendRequest, PRISON_USER_BIRMINGHAM)) doReturn Pair(
        probationBookingAtBirminghamPrison.apply {
          amendedTime = now()
          amendedBy = PRISON_USER_BIRMINGHAM.username
        },
        prisoner(prisonerNumber = prisoner.prisonerNumber, prisonCode = prisoner.prisonId),
      )
      whenever { videoBookingServiceDelegate.getVideoBookingById(probationBookingAtBirminghamPrison.videoBookingId, PRISON_USER_BIRMINGHAM) } doReturn probationBookingAtBirminghamPrison.toModel(setOf(birminghamLocation.toModel()))
      whenever(changeTrackingService.determineChangeType(probationBookingAtBirminghamPrison.videoBookingId, amendRequest, PRISON_USER_BIRMINGHAM)) doReturn ChangeType.PRISON

      facade.amend(probationBookingAtBirminghamPrison.videoBookingId, amendRequest, PRISON_USER_BIRMINGHAM)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailFacade, telemetryService) {
        verify(videoBookingServiceDelegate).amend(probationBookingAtBirminghamPrison.videoBookingId, amendRequest, PRISON_USER_BIRMINGHAM)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_AMENDED, probationBookingAtBirminghamPrison.videoBookingId)
        verify(emailFacade).sendEmails(bookingDetailsCaptor.capture(), userCaptor.capture(), changeTypeCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      bookingDetailsCaptor.singleValue isInstanceOf BookingDetails.Amended::class.java
      userCaptor.singleValue isEqualTo PRISON_USER_BIRMINGHAM
      changeTypeCaptor.singleValue isEqualTo ChangeType.PRISON

      with(telemetryCaptor.singleValue as ProbationBookingAmendedTelemetryEvent) {
        properties()["amended_by"] isEqualTo "prison"
      }
    }
  }

  @Nested
  @DisplayName("Prisoner transfers")
  inner class PrisonerTransfer {
    @Test
    fun `should send events and emails on transfer of prisoner by service user for a court booking`() {
      val prisoner = Prisoner(
        prisonerNumber = courtBooking.prisoner(),
        prisonId = "TRN",
        firstName = "Fred",
        lastName = "Bloggs",
        dateOfBirth = LocalDate.EPOCH,
        lastPrisonId = WANDSWORTH,
      )

      whenever(videoBookingServiceDelegate.cancel(courtBooking.videoBookingId, SERVICE_USER)) doReturn courtBooking.cancel(SERVICE_USER)
      whenever { videoBookingServiceDelegate.getVideoBookingById(courtBooking.videoBookingId, SERVICE_USER) } doReturn courtBooking.toModel(setOf(wandsworthLocation.toModel()))
      whenever(prisonerSearchClient.getPrisoner(courtBooking.prisoner())) doReturn prisoner

      facade.prisonerTransferred(courtBooking.videoBookingId, SERVICE_USER)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailFacade, telemetryService) {
        verify(videoBookingServiceDelegate).cancel(courtBooking.videoBookingId, SERVICE_USER)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, courtBooking.videoBookingId)
        verify(emailFacade).sendEmails(bookingDetailsCaptor.capture(), userCaptor.capture(), changeTypeCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      bookingDetailsCaptor.singleValue isInstanceOf BookingDetails.Transferred::class.java
      userCaptor.singleValue isEqualTo SERVICE_USER
      changeTypeCaptor.singleValue isEqualTo ChangeType.GLOBAL

      with(telemetryCaptor.singleValue as CourtBookingCancelledTelemetryEvent) {
        properties()["cancelled_by"] isEqualTo "transfer"
      }
    }

    @Test
    fun `should send events and emails on transfer of prisoner by service user for a probation booking`() {
      val prisoner = Prisoner(
        prisonerNumber = probationBookingAtBirminghamPrison.prisoner(),
        prisonId = "TRN",
        firstName = "Fred",
        lastName = "Bloggs",
        dateOfBirth = LocalDate.EPOCH,
        lastPrisonId = WANDSWORTH,
      )

      whenever(videoBookingServiceDelegate.cancel(probationBookingAtBirminghamPrison.videoBookingId, SERVICE_USER)) doReturn probationBookingAtBirminghamPrison.apply { cancel(SERVICE_USER) }
      whenever { videoBookingServiceDelegate.getVideoBookingById(probationBookingAtBirminghamPrison.videoBookingId, SERVICE_USER) } doReturn probationBookingAtBirminghamPrison.toModel(setOf(birminghamLocation.toModel()))
      whenever(prisonerSearchClient.getPrisoner(probationBookingAtBirminghamPrison.prisoner())) doReturn prisoner

      facade.prisonerTransferred(probationBookingAtBirminghamPrison.videoBookingId, SERVICE_USER)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailFacade, telemetryService) {
        verify(videoBookingServiceDelegate).cancel(probationBookingAtBirminghamPrison.videoBookingId, SERVICE_USER)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, probationBookingAtBirminghamPrison.videoBookingId)
        verify(emailFacade).sendEmails(bookingDetailsCaptor.capture(), userCaptor.capture(), changeTypeCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      bookingDetailsCaptor.singleValue isInstanceOf BookingDetails.Transferred::class.java
      userCaptor.singleValue isEqualTo SERVICE_USER
      changeTypeCaptor.singleValue isEqualTo ChangeType.GLOBAL

      with(telemetryCaptor.singleValue as ProbationBookingCancelledTelemetryEvent) {
        properties()["cancelled_by"] isEqualTo "transfer"
      }
    }
  }

  @Nested
  @DisplayName("Prisoner releases")
  inner class PrisonerRelease {
    @Test
    fun `should send events and emails on release of prisoner by service user for a court booking`() {
      val prisoner = Prisoner(
        prisonerNumber = courtBooking.prisoner(),
        prisonId = "TRN",
        firstName = "Fred",
        lastName = "Bloggs",
        dateOfBirth = LocalDate.EPOCH,
        lastPrisonId = WANDSWORTH,
      )

      whenever(videoBookingServiceDelegate.cancel(courtBooking.videoBookingId, SERVICE_USER)) doReturn courtBooking.apply { cancel(SERVICE_USER) }
      whenever { videoBookingServiceDelegate.getVideoBookingById(courtBooking.videoBookingId, SERVICE_USER) } doReturn courtBooking.toModel(setOf(wandsworthLocation.toModel()))
      whenever(prisonerSearchClient.getPrisoner(courtBooking.prisoner())) doReturn prisoner

      facade.prisonerReleased(courtBooking.videoBookingId, SERVICE_USER)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailFacade, telemetryService) {
        verify(videoBookingServiceDelegate).cancel(courtBooking.videoBookingId, SERVICE_USER)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, courtBooking.videoBookingId)
        verify(emailFacade).sendEmails(bookingDetailsCaptor.capture(), userCaptor.capture(), changeTypeCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      bookingDetailsCaptor.singleValue isInstanceOf BookingDetails.Released::class.java
      userCaptor.singleValue isEqualTo SERVICE_USER
      changeTypeCaptor.singleValue isEqualTo ChangeType.GLOBAL

      with(telemetryCaptor.singleValue as CourtBookingCancelledTelemetryEvent) {
        properties()["cancelled_by"] isEqualTo "release"
      }
    }

    @Test
    fun `should send events and emails on release of prisoner by service user for a probation booking`() {
      val prisoner = Prisoner(
        prisonerNumber = probationBookingAtBirminghamPrison.prisoner(),
        prisonId = "TRN",
        firstName = "Fred",
        lastName = "Bloggs",
        dateOfBirth = LocalDate.EPOCH,
        lastPrisonId = WANDSWORTH,
      )

      whenever(videoBookingServiceDelegate.cancel(probationBookingAtBirminghamPrison.videoBookingId, SERVICE_USER)) doReturn probationBookingAtBirminghamPrison.apply { cancel(SERVICE_USER) }
      whenever { videoBookingServiceDelegate.getVideoBookingById(probationBookingAtBirminghamPrison.videoBookingId, SERVICE_USER) } doReturn probationBookingAtBirminghamPrison.toModel(setOf(birminghamLocation.toModel()))
      whenever(prisonerSearchClient.getPrisoner(probationBookingAtBirminghamPrison.prisoner())) doReturn prisoner

      facade.prisonerReleased(probationBookingAtBirminghamPrison.videoBookingId, SERVICE_USER)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailFacade, telemetryService) {
        verify(videoBookingServiceDelegate).cancel(probationBookingAtBirminghamPrison.videoBookingId, SERVICE_USER)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, probationBookingAtBirminghamPrison.videoBookingId)
        verify(emailFacade).sendEmails(bookingDetailsCaptor.capture(), userCaptor.capture(), changeTypeCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      bookingDetailsCaptor.singleValue isInstanceOf BookingDetails.Released::class.java
      userCaptor.singleValue isEqualTo SERVICE_USER
      changeTypeCaptor.singleValue isEqualTo ChangeType.GLOBAL

      with(telemetryCaptor.singleValue as ProbationBookingCancelledTelemetryEvent) {
        properties()["cancelled_by"] isEqualTo "release"
      }
    }
  }

  @Nested
  @DisplayName("Court hearing link reminder")
  inner class CourtHearingLinkReminder {
    @Test
    fun `should send an email to the court contact to remind them to add a court hearing link`() {
      val courtBooking = courtBooking(cvpLinkDetails = null, notesForStaff = "court notes for staff")
        .addAppointment(
          prison = prison(prisonCode = WANDSWORTH),
          prisonerNumber = "123456",
          appointmentType = "VLB_COURT_MAIN",
          date = LocalDate.of(2100, 1, 1),
          startTime = LocalTime.of(11, 0),
          endTime = LocalTime.of(11, 30),
          locationId = wandsworthLocation.id,
        )

      val prisoner = Prisoner(
        prisonerNumber = courtBooking.prisoner(),
        prisonId = "WWI",
        firstName = "Fred",
        lastName = "Bloggs",
        dateOfBirth = LocalDate.EPOCH,
        lastPrisonId = WANDSWORTH,
      )

      whenever { videoBookingServiceDelegate.getVideoBookingById(courtBooking.videoBookingId, SERVICE_USER) } doReturn courtBooking.toModel(setOf(wandsworthLocation.toModel()))
      whenever(prisonerSearchClient.getPrisoner(courtBooking.prisoner())) doReturn prisoner

      facade.courtHearingLinkReminder(courtBooking, SERVICE_USER)
      verify(emailFacade).sendEmails(bookingDetailsCaptor.capture(), userCaptor.capture(), changeTypeCaptor.capture())

      bookingDetailsCaptor.singleValue isInstanceOf BookingDetails.CourtHearingLinkReminder::class.java
      userCaptor.singleValue isEqualTo SERVICE_USER
      changeTypeCaptor.singleValue isEqualTo ChangeType.GLOBAL
    }

    @Test
    fun `should throw an exception if the booking is not a court booking`() {
      val error = assertThrows<IllegalArgumentException> { facade.courtHearingLinkReminder(probationBookingAtBirminghamPrison, SERVICE_USER) }
      error.message isEqualTo "Video booking with id 0 must be an active court booking"
    }

    @Test
    fun `should throw an exception if the booking is not a active`() {
      val error = assertThrows<IllegalArgumentException> { facade.courtHearingLinkReminder(courtBooking.cancel(COURT_USER), SERVICE_USER) }
      error.message isEqualTo "Video booking with id 0 must be an active court booking"
    }

    @Test
    fun `should throw an exception if the court is not enabled`() {
      val error = assertThrows<IllegalArgumentException> { facade.courtHearingLinkReminder(courtBookingAtDisabledCourt, SERVICE_USER) }
      error.message isEqualTo "Video booking with id 0 is not with an enabled court"
    }

    @Test
    fun `should throw an exception if booking has already taken place`() {
      val error = assertThrows<IllegalArgumentException> { facade.courtHearingLinkReminder(courtBookingInThePast, SERVICE_USER) }
      error.message isEqualTo "Video booking with id 0 must be after today"
    }
  }

  @Nested
  @DisplayName("Probation officer details reminder")
  inner class ProbationOfficerDetailsReminder {
    @Test
    fun `should send an email to the probation contact to remind them to add missing probation officer details`() {
      val prisoner = Prisoner(
        prisonerNumber = probationBookingAtBirminghamPrison.prisoner(),
        prisonId = "WWI",
        firstName = "Fred",
        lastName = "Bloggs",
        dateOfBirth = LocalDate.EPOCH,
        lastPrisonId = BIRMINGHAM,
      )

      whenever { videoBookingServiceDelegate.getVideoBookingById(probationBookingAtBirminghamPrison.videoBookingId, SERVICE_USER) } doReturn probationBookingAtBirminghamPrison.toModel(setOf(birminghamLocation.toModel()))
      whenever(prisonerSearchClient.getPrisoner(probationBookingAtBirminghamPrison.prisoner())) doReturn prisoner

      facade.sendProbationOfficerDetailsReminder(probationBookingAtBirminghamPrison, SERVICE_USER)
      verify(emailFacade).sendEmails(bookingDetailsCaptor.capture(), userCaptor.capture(), changeTypeCaptor.capture())

      bookingDetailsCaptor.singleValue isInstanceOf BookingDetails.ProbationOfficerDetailsReminder::class.java
      userCaptor.singleValue isEqualTo SERVICE_USER
      changeTypeCaptor.singleValue isEqualTo ChangeType.GLOBAL
    }

    @Test
    fun `should throw an exception if the booking is not a probation booking`() {
      val error = assertThrows<IllegalArgumentException> { facade.sendProbationOfficerDetailsReminder(courtBooking, SERVICE_USER) }
      error.message isEqualTo "Video booking with id 0 must be an active probation booking"
    }

    @Test
    fun `should throw an exception if the booking is not an active probation booking`() {
      val error = assertThrows<IllegalArgumentException> { facade.sendProbationOfficerDetailsReminder(probationBooking().cancel(PROBATION_USER), SERVICE_USER) }
      error.message isEqualTo "Video booking with id 0 must be an active probation booking"
    }

    @Test
    fun `should throw an exception if the probation team is not enabled`() {
      val error = assertThrows<IllegalArgumentException> { facade.sendProbationOfficerDetailsReminder(probationBooking(probationTeam(enabled = false)), SERVICE_USER) }
      error.message isEqualTo "Video booking with id 0 is not with an enabled probation team"
    }

    @Test
    fun `should throw an exception if probation booking has already taken place`() {
      assertThrows<IllegalArgumentException> { facade.sendProbationOfficerDetailsReminder(probationBooking().withProbationPrisonAppointment(yesterday()), SERVICE_USER) }
        .message isEqualTo "Video booking with id 0 must be after today"

      assertThrows<IllegalArgumentException> { facade.sendProbationOfficerDetailsReminder(probationBooking().withProbationPrisonAppointment(today()), SERVICE_USER) }
        .message isEqualTo "Video booking with id 0 must be after today"
    }
  }
}

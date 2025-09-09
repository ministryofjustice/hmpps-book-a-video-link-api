package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toMediumFormatStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.VideoBookingEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Notification
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.SERVICE_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.additionalDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendCourtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendProbationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.bookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsEntriesExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isInstanceOf
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.locationAttributes
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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.AdditionalBookingDetailRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.AmendedCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.AmendedCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.AmendedCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CancelledCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CancelledCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CancelledCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.CourtHearingLinkReminderEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.NewCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.NewCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.NewCourtBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.ReleasedCourtBookingCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.ReleasedCourtBookingPrisonCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.TransferredCourtBookingPrisonNoCourtEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.AmendedProbationBookingPrisonNoProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.AmendedProbationBookingProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.AmendedProbationBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.CancelledProbationBookingPrisonNoProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.CancelledProbationBookingProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.CancelledProbationBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.NewProbationBookingPrisonNoProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.NewProbationBookingProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.NewProbationBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ProbationOfficerDetailsReminderEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ReleasedProbationBookingPrisonProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ReleasedProbationBookingProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.TransferredProbationBookingPrisonProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.TransferredProbationBookingProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.OutboundEventsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService
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
import java.util.UUID

class BookingFacadeTest {
  private val emailCaptor = argumentCaptor<VideoBookingEmail>()
  private val videoBookingServiceDelegate: VideoBookingServiceDelegate = mock()
  private val contactsService: ContactsService = mock()
  private val prisonRepository: PrisonRepository = mock()
  private val emailService: EmailService = mock()
  private val notificationRepository: NotificationRepository = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val notificationCaptor = argumentCaptor<Notification>()
  private val locationsService: LocationsService = mock()
  private val prisonerSearchClient: PrisonerSearchClient = mock()
  private val telemetryService: TelemetryService = mock()
  private val telemetryCaptor = argumentCaptor<TelemetryEvent>()
  private val availabilityService: AvailabilityService = mock()
  private val additionalBookingDetailRepository: AdditionalBookingDetailRepository = mock()
  private val changeTrackingService: ChangeTrackingService = mock()
  private val facade = BookingFacade(
    videoBookingServiceDelegate,
    contactsService,
    prisonRepository,
    emailService,
    notificationRepository,
    outboundEventsService,
    locationsService,
    prisonerSearchClient,
    telemetryService,
    availabilityService,
    additionalBookingDetailRepository,
    changeTrackingService,
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

  private val emailNotificationId = UUID.randomUUID()

  @BeforeEach
  fun before() {
    whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn prison(WANDSWORTH)
    whenever(prisonRepository.findByCode(BIRMINGHAM)) doReturn prison(BIRMINGHAM)
    whenever(locationsService.getLocationById(wandsworthLocation.id)) doReturn wandsworthLocation.toModel()
    whenever(locationsService.getLocationByKey(birminghamLocation.key)) doReturn birminghamLocation.toModel()
    whenever(locationsService.getLocationById(birminghamLocation.id)) doReturn birminghamLocation.toModel(locationAttributes().copy(prisonVideoUrl = "birmingham-video-url"))
    whenever(availabilityService.isAvailable(any<CreateVideoBookingRequest>())) doReturn true
    whenever(availabilityService.isAvailable(anyLong(), any())) doReturn true
    whenever(changeTrackingService.hasBookingChanged(any(), any(), any())) doReturn true
  }

  @Nested
  @DisplayName("Create bookings")
  inner class CreateBooking {
    @Test
    fun `should send events and emails on creation of court booking by court user`() {
      setupCourtPrimaryContactsFor(COURT_USER)

      val bookingRequest = courtBookingRequest(prisonCode = WANDSWORTH, prisonerNumber = courtBooking.prisoner())

      whenever(videoBookingServiceDelegate.create(bookingRequest, COURT_USER)) doReturn Pair(courtBooking, prisoner(prisonerNumber = courtBooking.prisoner(), prisonCode = WANDSWORTH))
      whenever(emailService.send(any<NewCourtBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<NewCourtBookingPrisonNoCourtEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.create(bookingRequest, COURT_USER)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailService, notificationRepository, telemetryService) {
        verify(videoBookingServiceDelegate).create(bookingRequest, COURT_USER)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CREATED, courtBooking.videoBookingId)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf NewCourtBookingUserEmail::class.java
        address isEqualTo COURT_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to COURT_USER.name,
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Fred Bloggs",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
          "courtHearingLink" to "https://court.hearing.link",
          "prePrisonVideoUrl" to "",
          "postPrisonVideoUrl" to "",
        )
      }
      with(emailCaptor.secondValue) {
        this isInstanceOf NewCourtBookingPrisonNoCourtEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Fred Bloggs",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
          "courtHearingLink" to "https://court.hearing.link",
          "prePrisonVideoUrl" to "",
          "postPrisonVideoUrl" to "",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo COURT_USER.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "CREATE"
      }
      with(notificationCaptor.secondValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "prison template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "CREATE"
      }

      with(telemetryCaptor.firstValue as CourtBookingCreatedTelemetryEvent) {
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
      setupCourtPrimaryContactsFor(PRISON_USER_BIRMINGHAM)

      val bookingRequest = courtBookingRequest(prisonCode = WANDSWORTH, prisonerNumber = courtBookingCreatedByPrison.prisoner())

      whenever(videoBookingServiceDelegate.create(bookingRequest, PRISON_USER_BIRMINGHAM)) doReturn Pair(courtBookingCreatedByPrison, prisoner(prisonerNumber = courtBookingCreatedByPrison.prisoner(), prisonCode = WANDSWORTH))
      whenever(emailService.send(any<NewCourtBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<NewCourtBookingCourtEmail>())) doReturn Result.success(emailNotificationId to "court template id")

      facade.create(bookingRequest, PRISON_USER_BIRMINGHAM)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailService, notificationRepository, telemetryService) {
        verify(videoBookingServiceDelegate).create(bookingRequest, PRISON_USER_BIRMINGHAM)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CREATED, courtBookingCreatedByPrison.videoBookingId)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf NewCourtBookingUserEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to PRISON_USER_BIRMINGHAM.name,
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Fred Bloggs",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
          "courtHearingLink" to "https://court.hearing.link",
          "prePrisonVideoUrl" to "",
          "postPrisonVideoUrl" to "",
        )
      }
      with(emailCaptor.secondValue) {
        this isInstanceOf NewCourtBookingCourtEmail::class.java
        address isEqualTo COURT_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Fred Bloggs",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
          "courtHearingLink" to "https://court.hearing.link",
          "prePrisonVideoUrl" to "",
          "postPrisonVideoUrl" to "",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBookingCreatedByPrison
        reason isEqualTo "CREATE"
      }
      with(notificationCaptor.secondValue) {
        email isEqualTo COURT_USER.email
        templateName isEqualTo "court template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBookingCreatedByPrison
        reason isEqualTo "CREATE"
      }

      with(telemetryCaptor.firstValue as CourtBookingCreatedTelemetryEvent) {
        properties()["created_by"] isEqualTo "prison"
      }
    }

    @Test
    fun `should send events and emails on creation of probation booking by probation user`() {
      setupProbationPrimaryContacts(PROBATION_USER)

      val prisoner = Prisoner(probationBookingAtBirminghamPrison.prisoner(), BIRMINGHAM, "Bob", "Builder", yesterday())

      whenever(prisonerSearchClient.getPrisoner(prisoner.prisonerNumber)) doReturn prisoner

      val request = probationBookingRequest(
        prisonCode = prisoner.prisonId!!,
        prisonerNumber = prisoner.prisonerNumber,
        appointmentDate = tomorrow(),
        startTime = LocalTime.MIDNIGHT,
        endTime = LocalTime.MIDNIGHT.plusHours(1),
      )

      whenever(videoBookingServiceDelegate.create(request, PROBATION_USER)) doReturn Pair(probationBookingAtBirminghamPrison, prisoner(prisonCode = prisoner.prisonId!!, prisonerNumber = prisoner.prisonerNumber, firstName = prisoner.firstName, lastName = prisoner.lastName))
      whenever(emailService.send(any<NewProbationBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<NewProbationBookingPrisonNoProbationEmail>())) doReturn Result.success(emailNotificationId to "probation template id")
      whenever(additionalBookingDetailRepository.findByVideoBooking(probationBookingAtBirminghamPrison)) doReturn additionalDetails(probationBookingAtBirminghamPrison, "probation officer name", "probation.officer@email.com", "0114 2345678")

      facade.create(request, PROBATION_USER)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailService, notificationRepository, telemetryService) {
        verify(videoBookingServiceDelegate).create(request, PROBATION_USER)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CREATED, probationBookingAtBirminghamPrison.videoBookingId)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      emailCaptor.allValues hasSize 2

      with(emailCaptor.firstValue) {
        this isInstanceOf NewProbationBookingUserEmail::class.java
        address isEqualTo PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to PROBATION_USER.name,
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "probation officer name",
          "probationOfficerEmailAddress" to "probation.officer@email.com",
          "probationOfficerContactNumber" to "0114 2345678",
        )
      }

      with(emailCaptor.secondValue) {
        this isInstanceOf NewProbationBookingPrisonNoProbationEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "probation officer name",
          "probationOfficerEmailAddress" to "probation.officer@email.com",
          "probationOfficerContactNumber" to "0114 2345678",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PROBATION_USER.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "CREATE"
      }

      with(notificationCaptor.secondValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "probation template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "CREATE"
      }

      with(telemetryCaptor.firstValue as ProbationBookingCreatedTelemetryEvent) {
        properties()["created_by"] isEqualTo "probation"
      }
    }

    @Test
    fun `should send events and emails on creation of probation booking by prison user`() {
      setupProbationPrimaryContacts(PRISON_USER_BIRMINGHAM)

      val prisoner = Prisoner(probationBookingAtBirminghamPrison.prisoner(), BIRMINGHAM, "Bob", "Builder", yesterday())

      whenever(prisonerSearchClient.getPrisoner(prisoner.prisonerNumber)) doReturn prisoner

      val request = probationBookingRequest(
        prisonCode = prisoner.prisonId!!,
        prisonerNumber = prisoner.prisonerNumber,
        appointmentDate = tomorrow(),
        startTime = LocalTime.MIDNIGHT,
        endTime = LocalTime.MIDNIGHT.plusHours(1),
      )

      whenever(videoBookingServiceDelegate.create(request, PRISON_USER_BIRMINGHAM)) doReturn Pair(probationBookingAtBirminghamPrisonCreatedByPrison, prisoner(prisonCode = prisoner.prisonId!!, prisonerNumber = prisoner.prisonerNumber, firstName = prisoner.firstName, lastName = prisoner.lastName))
      whenever(emailService.send(any<NewProbationBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<NewProbationBookingProbationEmail>())) doReturn Result.success(emailNotificationId to "probation template id")
      whenever(additionalBookingDetailRepository.findByVideoBooking(probationBookingAtBirminghamPrison)) doReturn additionalDetails(probationBookingAtBirminghamPrison, "probation officer name", "probation.officer@email.com", "0114 2345678")

      facade.create(request, PRISON_USER_BIRMINGHAM)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailService, notificationRepository, telemetryService) {
        verify(videoBookingServiceDelegate).create(request, PRISON_USER_BIRMINGHAM)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CREATED, probationBookingAtBirminghamPrison.videoBookingId)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      emailCaptor.allValues hasSize 2

      with(emailCaptor.firstValue) {
        this isInstanceOf NewProbationBookingUserEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to PRISON_USER_BIRMINGHAM.name,
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "probation officer name",
          "probationOfficerEmailAddress" to "probation.officer@email.com",
          "probationOfficerContactNumber" to "0114 2345678",
        )
      }

      with(emailCaptor.secondValue) {
        this isInstanceOf NewProbationBookingProbationEmail::class.java
        address isEqualTo PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "probation officer name",
          "probationOfficerEmailAddress" to "probation.officer@email.com",
          "probationOfficerContactNumber" to "0114 2345678",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "CREATE"
      }

      with(notificationCaptor.secondValue) {
        email isEqualTo PROBATION_USER.email
        templateName isEqualTo "probation template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "CREATE"
      }

      with(telemetryCaptor.firstValue as ProbationBookingCreatedTelemetryEvent) {
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
      setupCourtPrimaryContactsFor(COURT_USER)

      val prisoner = Prisoner(courtBooking.prisoner(), WANDSWORTH, "Bob", "Builder", yesterday())

      whenever(videoBookingServiceDelegate.cancel(1, COURT_USER)) doReturn courtBooking.apply { cancel(COURT_USER) }
      whenever(prisonerSearchClient.getPrisoner(courtBooking.prisoner())) doReturn prisoner
      whenever(emailService.send(any<CancelledCourtBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<CancelledCourtBookingPrisonNoCourtEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.cancel(1, COURT_USER)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailService, notificationRepository, telemetryService) {
        verify(videoBookingServiceDelegate).cancel(1, COURT_USER)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, 1)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf CancelledCourtBookingUserEmail::class.java
        address isEqualTo COURT_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to COURT_USER.name,
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Bob Builder",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
          "courtHearingLink" to "https://court.hearing.link",
        )
      }
      with(emailCaptor.secondValue) {
        this isInstanceOf CancelledCourtBookingPrisonNoCourtEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Bob Builder",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
          "courtHearingLink" to "https://court.hearing.link",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo COURT_USER.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "CANCEL"
      }
      with(notificationCaptor.secondValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "prison template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "CANCEL"
      }

      with(telemetryCaptor.firstValue as CourtBookingCancelledTelemetryEvent) {
        properties()["cancelled_by"] isEqualTo "court"
      }
    }

    @Test
    fun `should send events and emails on cancellation of court booking by prison user`() {
      setupCourtPrimaryContactsFor(PRISON_USER_BIRMINGHAM)

      val prisoner = Prisoner(courtBooking.prisoner(), WANDSWORTH, "Bob", "Builder", yesterday())

      whenever(videoBookingServiceDelegate.cancel(1, PRISON_USER_BIRMINGHAM)) doReturn courtBooking.apply { cancel(PRISON_USER_BIRMINGHAM) }
      whenever(prisonerSearchClient.getPrisoner(courtBooking.prisoner())) doReturn prisoner
      whenever(emailService.send(any<CancelledCourtBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<CancelledCourtBookingCourtEmail>())) doReturn Result.success(emailNotificationId to "court template id")

      facade.cancel(1, PRISON_USER_BIRMINGHAM)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailService, notificationRepository, telemetryService) {
        verify(videoBookingServiceDelegate).cancel(1, PRISON_USER_BIRMINGHAM)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, 1)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf CancelledCourtBookingUserEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to PRISON_USER_BIRMINGHAM.name,
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Bob Builder",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
          "courtHearingLink" to "https://court.hearing.link",
        )
      }
      with(emailCaptor.secondValue) {
        this isInstanceOf CancelledCourtBookingCourtEmail::class.java
        address isEqualTo COURT_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Bob Builder",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
          "courtHearingLink" to "https://court.hearing.link",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "CANCEL"
      }
      with(notificationCaptor.secondValue) {
        email isEqualTo COURT_USER.email
        templateName isEqualTo "court template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "CANCEL"
      }

      with(telemetryCaptor.firstValue as CourtBookingCancelledTelemetryEvent) {
        properties()["cancelled_by"] isEqualTo "prison"
      }
    }

    @Test
    fun `should send emails but no events on cancellation of a court booking by service user`() {
      contactsService.stub {
        on { getBookingContacts(any(), eq(SERVICE_USER)) } doReturn listOf(
          bookingContact(contactType = ContactType.COURT, email = COURT_USER.email, name = COURT_USER.name),
        )
      }

      val prisoner = Prisoner(courtBooking.prisoner(), WANDSWORTH, "Bob", "Builder", yesterday())

      whenever(videoBookingServiceDelegate.cancel(1, SERVICE_USER)) doReturn courtBooking.apply { cancel(SERVICE_USER) }
      whenever(prisonerSearchClient.getPrisoner(courtBooking.prisoner())) doReturn prisoner
      whenever(emailService.send(any<CancelledCourtBookingCourtEmail>())) doReturn Result.success(emailNotificationId to "court template id")

      facade.cancel(1, SERVICE_USER)

      inOrder(videoBookingServiceDelegate, emailService, notificationRepository, telemetryService) {
        verify(videoBookingServiceDelegate).cancel(1, SERVICE_USER)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      verifyNoInteractions(outboundEventsService)

      with(emailCaptor.allValues.single()) {
        this isInstanceOf CancelledCourtBookingCourtEmail::class.java
        address isEqualTo COURT_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Bob Builder",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
          "courtHearingLink" to "https://court.hearing.link",
        )
      }

      with(notificationCaptor.allValues.single()) {
        email isEqualTo COURT_USER.email
        templateName isEqualTo "court template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "CANCEL"
      }

      with(telemetryCaptor.firstValue as CourtBookingCancelledTelemetryEvent) {
        properties()["cancelled_by"] isEqualTo "prison"
      }
    }

    @Test
    fun `should send events and emails on cancellation of probation booking by probation user`() {
      setupProbationPrimaryContacts(PROBATION_USER)

      val prisoner = Prisoner(probationBookingAtBirminghamPrison.prisoner(), BIRMINGHAM, "Bob", "Builder", yesterday())

      whenever(prisonerSearchClient.getPrisoner(prisoner.prisonerNumber)) doReturn prisoner
      whenever(videoBookingServiceDelegate.cancel(1, PROBATION_USER)) doReturn probationBookingAtBirminghamPrison.apply { cancel(PROBATION_USER) }
      whenever(emailService.send(any<CancelledProbationBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<CancelledProbationBookingPrisonNoProbationEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.cancel(1, PROBATION_USER)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailService, notificationRepository, telemetryService) {
        verify(videoBookingServiceDelegate).cancel(1, PROBATION_USER)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, 1)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf CancelledProbationBookingUserEmail::class.java
        address isEqualTo PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "userName" to PROBATION_USER.name,
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "Not yet known",
          "probationOfficerEmailAddress" to "Not yet known",
          "probationOfficerContactNumber" to "Not yet known",
        )
      }

      with(emailCaptor.secondValue) {
        this isInstanceOf CancelledProbationBookingPrisonNoProbationEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "Not yet known",
          "probationOfficerEmailAddress" to "Not yet known",
          "probationOfficerContactNumber" to "Not yet known",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PROBATION_USER.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "CANCEL"
      }
      with(notificationCaptor.secondValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "prison template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "CANCEL"
      }

      with(telemetryCaptor.firstValue as ProbationBookingCancelledTelemetryEvent) {
        properties()["cancelled_by"] isEqualTo "probation"
      }
    }

    @Test
    fun `should send events and emails on cancellation of probation booking by prison user`() {
      setupProbationPrimaryContacts(PRISON_USER_BIRMINGHAM)

      val prisoner = Prisoner(probationBookingAtBirminghamPrison.prisoner(), BIRMINGHAM, "Bob", "Builder", yesterday())

      whenever(prisonerSearchClient.getPrisoner(prisoner.prisonerNumber)) doReturn prisoner
      whenever(videoBookingServiceDelegate.cancel(1, PRISON_USER_BIRMINGHAM)) doReturn probationBookingAtBirminghamPrison.apply { cancel(PRISON_USER_BIRMINGHAM) }
      whenever(emailService.send(any<CancelledProbationBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<CancelledProbationBookingProbationEmail>())) doReturn Result.success(emailNotificationId to "probation template id")

      facade.cancel(1, PRISON_USER_BIRMINGHAM)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailService, notificationRepository, telemetryService) {
        verify(videoBookingServiceDelegate).cancel(1, PRISON_USER_BIRMINGHAM)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, 1)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf CancelledProbationBookingUserEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to PRISON_USER_BIRMINGHAM.name,
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "Not yet known",
          "probationOfficerEmailAddress" to "Not yet known",
          "probationOfficerContactNumber" to "Not yet known",
        )
      }

      with(emailCaptor.secondValue) {
        this isInstanceOf CancelledProbationBookingProbationEmail::class.java
        address isEqualTo PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "Not yet known",
          "probationOfficerEmailAddress" to "Not yet known",
          "probationOfficerContactNumber" to "Not yet known",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "CANCEL"
      }
      with(notificationCaptor.secondValue) {
        email isEqualTo PROBATION_USER.email
        templateName isEqualTo "probation template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "CANCEL"
      }

      with(telemetryCaptor.firstValue as ProbationBookingCancelledTelemetryEvent) {
        properties()["cancelled_by"] isEqualTo "prison"
      }
    }

    @Test
    fun `should send emails but no events on cancellation of a probation booking by service user`() {
      contactsService.stub {
        on { getBookingContacts(any(), eq(SERVICE_USER)) } doReturn listOf(
          bookingContact(contactType = ContactType.PROBATION, email = PROBATION_USER.email, name = PROBATION_USER.name),
        )
      }

      val prisoner = Prisoner(probationBookingAtBirminghamPrison.prisoner(), BIRMINGHAM, "Bob", "Builder", yesterday())

      whenever(prisonerSearchClient.getPrisoner(prisoner.prisonerNumber)) doReturn prisoner
      whenever(videoBookingServiceDelegate.cancel(1, SERVICE_USER)) doReturn probationBookingAtBirminghamPrison.apply { cancel(SERVICE_USER) }
      whenever(emailService.send(any<CancelledProbationBookingProbationEmail>())) doReturn Result.success(emailNotificationId to "probation template id")

      facade.cancel(1, SERVICE_USER)

      inOrder(videoBookingServiceDelegate, emailService, notificationRepository, telemetryService) {
        verify(videoBookingServiceDelegate).cancel(1, SERVICE_USER)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      verifyNoInteractions(outboundEventsService)

      with(emailCaptor.allValues.single()) {
        this isInstanceOf CancelledProbationBookingProbationEmail::class.java
        address isEqualTo PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "Not yet known",
          "probationOfficerEmailAddress" to "Not yet known",
          "probationOfficerContactNumber" to "Not yet known",
        )
      }

      with(notificationCaptor.allValues.single()) {
        email isEqualTo PROBATION_USER.email
        templateName isEqualTo "probation template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "CANCEL"
      }

      with(telemetryCaptor.firstValue as ProbationBookingCancelledTelemetryEvent) {
        properties()["cancelled_by"] isEqualTo "prison"
      }
    }
  }

  @Nested
  @DisplayName("Amend bookings")
  inner class AmendBooking {
    @Test
    fun `should send events and emails on amendment of court booking by court user`() {
      setupCourtPrimaryContactsFor(COURT_USER)

      val bookingRequest = amendCourtBookingRequest(prisonCode = WANDSWORTH, prisonerNumber = "123456")

      whenever(
        videoBookingServiceDelegate.amend(
          1,
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

      whenever(emailService.send(any<AmendedCourtBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<AmendedCourtBookingPrisonNoCourtEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.amend(1, bookingRequest, COURT_USER)

      inOrder(videoBookingServiceDelegate, emailService, notificationRepository, telemetryService) {
        verify(videoBookingServiceDelegate).amend(1, bookingRequest, COURT_USER)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf AmendedCourtBookingUserEmail::class.java
        address isEqualTo COURT_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to COURT_USER.name,
          "court" to DERBY_JUSTICE_CENTRE,
          "offenderNo" to "123456",
          "prisonerName" to "Fred Bloggs",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
          "prison" to "Wandsworth",
          "courtHearingLink" to "https://court.hearing.link",
          "prePrisonVideoUrl" to "",
          "postPrisonVideoUrl" to "",
        )
      }
      with(emailCaptor.secondValue) {
        this isInstanceOf AmendedCourtBookingPrisonNoCourtEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Fred Bloggs",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
          "courtHearingLink" to "https://court.hearing.link",
          "prePrisonVideoUrl" to "",
          "postPrisonVideoUrl" to "",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo COURT_USER.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "AMEND"
      }
      with(notificationCaptor.secondValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "prison template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "AMEND"
      }

      with(telemetryCaptor.firstValue as CourtBookingAmendedTelemetryEvent) {
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
      setupCourtPrimaryContactsFor(PRISON_USER_BIRMINGHAM)

      val bookingRequest = amendCourtBookingRequest(prisonCode = WANDSWORTH, prisonerNumber = "123456")

      whenever(videoBookingServiceDelegate.amend(1, bookingRequest, PRISON_USER_BIRMINGHAM)) doReturn
        Pair(
          courtBooking.apply {
            amendedTime = now()
            amendedBy = PRISON_USER_BIRMINGHAM.username
          },
          prisoner(prisonerNumber = "123456", prisonCode = WANDSWORTH),
        )

      whenever(emailService.send(any<AmendedCourtBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<AmendedCourtBookingCourtEmail>())) doReturn Result.success(emailNotificationId to "court template id")

      facade.amend(1, bookingRequest, PRISON_USER_BIRMINGHAM)

      inOrder(videoBookingServiceDelegate, emailService, notificationRepository, telemetryService) {
        verify(videoBookingServiceDelegate).amend(1, bookingRequest, PRISON_USER_BIRMINGHAM)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf AmendedCourtBookingUserEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to PRISON_USER_BIRMINGHAM.name,
          "court" to DERBY_JUSTICE_CENTRE,
          "offenderNo" to "123456",
          "prisonerName" to "Fred Bloggs",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
          "prison" to "Wandsworth",
          "courtHearingLink" to "https://court.hearing.link",
          "prePrisonVideoUrl" to "",
          "postPrisonVideoUrl" to "",
        )
      }
      with(emailCaptor.secondValue) {
        this isInstanceOf AmendedCourtBookingCourtEmail::class.java
        address isEqualTo COURT_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Fred Bloggs",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
          "courtHearingLink" to "https://court.hearing.link",
          "prePrisonVideoUrl" to "",
          "postPrisonVideoUrl" to "",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "AMEND"
      }
      with(notificationCaptor.secondValue) {
        email isEqualTo COURT_USER.email
        templateName isEqualTo "court template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "AMEND"
      }

      with(telemetryCaptor.firstValue as CourtBookingAmendedTelemetryEvent) {
        properties()["amended_by"] isEqualTo "prison"
      }
    }

    @Test
    fun `should send events and emails on amendment of probation booking by probation user`() {
      setupProbationPrimaryContacts(PROBATION_USER)

      val prisoner = Prisoner(probationBookingAtBirminghamPrison.prisoner(), BIRMINGHAM, "Bob", "Builder", yesterday())

      whenever(prisonerSearchClient.getPrisoner(prisoner.prisonerNumber)) doReturn prisoner

      val amendRequest = amendProbationBookingRequest(prisonCode = prisoner.prisonId!!, prisonerNumber = prisoner.prisonerNumber, appointmentDate = tomorrow())

      whenever(videoBookingServiceDelegate.amend(1, amendRequest, PROBATION_USER)) doReturn Pair(
        probationBookingAtBirminghamPrison.apply {
          amendedTime = now()
          amendedBy = PROBATION_USER.username
        },
        prisoner(prisonerNumber = prisoner.prisonerNumber, prisonCode = prisoner.prisonId!!),
      )
      whenever(emailService.send(any<AmendedProbationBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<AmendedProbationBookingPrisonNoProbationEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.amend(1, amendRequest, PROBATION_USER)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailService, notificationRepository, telemetryService) {
        verify(videoBookingServiceDelegate).amend(1, amendRequest, PROBATION_USER)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_AMENDED, 1)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf AmendedProbationBookingUserEmail::class.java
        address isEqualTo PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "userName" to PROBATION_USER.name,
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Fred Bloggs",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "Not yet known",
          "probationOfficerEmailAddress" to "Not yet known",
          "probationOfficerContactNumber" to "Not yet known",
        )
      }
      with(emailCaptor.secondValue) {
        this isInstanceOf AmendedProbationBookingPrisonNoProbationEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Fred Bloggs",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "Not yet known",
          "probationOfficerEmailAddress" to "Not yet known",
          "probationOfficerContactNumber" to "Not yet known",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PROBATION_USER.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "AMEND"
      }
      with(notificationCaptor.secondValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "prison template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "AMEND"
      }

      with(telemetryCaptor.firstValue as ProbationBookingAmendedTelemetryEvent) {
        properties()["amended_by"] isEqualTo "probation"
      }
    }

    @Test
    fun `should send events and emails on amendment of probation booking by prison user`() {
      setupProbationPrimaryContacts(PRISON_USER_BIRMINGHAM)

      val prisoner = Prisoner(probationBookingAtBirminghamPrison.prisoner(), BIRMINGHAM, "Bob", "Builder", yesterday())

      whenever(prisonerSearchClient.getPrisoner(prisoner.prisonerNumber)) doReturn prisoner

      val amendRequest = amendProbationBookingRequest(prisonCode = prisoner.prisonId!!, prisonerNumber = prisoner.prisonerNumber, appointmentDate = tomorrow())

      whenever(videoBookingServiceDelegate.amend(1, amendRequest, PRISON_USER_BIRMINGHAM)) doReturn Pair(
        probationBookingAtBirminghamPrison.apply {
          amendedTime = now()
          amendedBy = PRISON_USER_BIRMINGHAM.username
        },
        prisoner(prisonerNumber = prisoner.prisonerNumber, prisonCode = prisoner.prisonId!!),
      )
      whenever(emailService.send(any<AmendedProbationBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<AmendedProbationBookingProbationEmail>())) doReturn Result.success(emailNotificationId to "probation template id")

      facade.amend(1, amendRequest, PRISON_USER_BIRMINGHAM)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailService, notificationRepository, telemetryService) {
        verify(videoBookingServiceDelegate).amend(1, amendRequest, PRISON_USER_BIRMINGHAM)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_AMENDED, 1)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf AmendedProbationBookingUserEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to PRISON_USER_BIRMINGHAM.name,
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Fred Bloggs",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "Not yet known",
          "probationOfficerEmailAddress" to "Not yet known",
          "probationOfficerContactNumber" to "Not yet known",
        )
      }
      with(emailCaptor.secondValue) {
        this isInstanceOf AmendedProbationBookingProbationEmail::class.java
        address isEqualTo PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Fred Bloggs",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "Not yet known",
          "probationOfficerEmailAddress" to "Not yet known",
          "probationOfficerContactNumber" to "Not yet known",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "user template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "AMEND"
      }
      with(notificationCaptor.secondValue) {
        email isEqualTo PROBATION_USER.email
        templateName isEqualTo "probation template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "AMEND"
      }

      with(telemetryCaptor.firstValue as ProbationBookingAmendedTelemetryEvent) {
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
    fun `should be minimal service interactions when no actual changes`() {
      val amendRequest = amendProbationBookingRequest()

      whenever(changeTrackingService.hasBookingChanged(1, amendRequest, PROBATION_USER)) doReturn false

      facade.amend(1, amendRequest, PROBATION_USER)

      verify(changeTrackingService).hasBookingChanged(1, amendRequest, PROBATION_USER)
      verifyNoInteractions(videoBookingServiceDelegate)
      verifyNoInteractions(outboundEventsService)
      verifyNoInteractions(emailService)
      verifyNoInteractions(notificationRepository)
      verifyNoInteractions(telemetryService)
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
        firstName = "Bob",
        lastName = "Builder",
        dateOfBirth = LocalDate.EPOCH,
        lastPrisonId = WANDSWORTH,
      )
      whenever(contactsService.getBookingContacts(any(), anyOrNull())) doReturn listOf(
        bookingContact(contactType = ContactType.PRISON, email = "jon@prison.com", name = "Jon"),
      )

      whenever(videoBookingServiceDelegate.cancel(1, SERVICE_USER)) doReturn courtBooking.apply { cancel(SERVICE_USER) }
      whenever(prisonerSearchClient.getPrisoner(courtBooking.prisoner())) doReturn prisoner
      whenever(emailService.send(any<TransferredCourtBookingPrisonNoCourtEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.prisonerTransferred(1, SERVICE_USER)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailService, notificationRepository, telemetryService) {
        verify(videoBookingServiceDelegate).cancel(1, SERVICE_USER)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, 1)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      emailCaptor.allValues hasSize 1
      with(emailCaptor.firstValue) {
        this isInstanceOf TransferredCourtBookingPrisonNoCourtEmail::class.java
        address isEqualTo "jon@prison.com"
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Bob Builder",
          "dateOfBirth" to LocalDate.EPOCH.toMediumFormatStyle(),
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
        )
      }

      notificationCaptor.allValues hasSize 1
      with(notificationCaptor.firstValue) {
        email isEqualTo "jon@prison.com"
        templateName isEqualTo "prison template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "TRANSFERRED"
      }

      with(telemetryCaptor.firstValue as CourtBookingCancelledTelemetryEvent) {
        properties()["cancelled_by"] isEqualTo "transfer"
      }
    }

    @Test
    fun `should send events and emails on transfer of prisoner by service user for a probation booking`() {
      val prisoner = Prisoner(
        prisonerNumber = probationBookingAtBirminghamPrison.prisoner(),
        prisonId = "TRN",
        firstName = "Bob",
        lastName = "Builder",
        dateOfBirth = LocalDate.EPOCH,
        lastPrisonId = WANDSWORTH,
      )

      setupProbationPrimaryContacts(SERVICE_USER)

      whenever(videoBookingServiceDelegate.cancel(1, SERVICE_USER)) doReturn probationBookingAtBirminghamPrison.apply { cancel(SERVICE_USER) }
      whenever(prisonerSearchClient.getPrisoner(probationBookingAtBirminghamPrison.prisoner())) doReturn prisoner
      whenever(emailService.send(any<TransferredProbationBookingProbationEmail>())) doReturn Result.success(emailNotificationId to "probation template id")
      whenever(emailService.send(any<TransferredProbationBookingPrisonProbationEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.prisonerTransferred(1, SERVICE_USER)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailService, notificationRepository, telemetryService) {
        verify(videoBookingServiceDelegate).cancel(1, SERVICE_USER)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, 1)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf TransferredProbationBookingPrisonProbationEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "probationEmailAddress" to "probation.user@probation.com",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "dateOfBirth" to LocalDate.EPOCH.toMediumFormatStyle(),
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "Not yet known",
          "probationOfficerEmailAddress" to "Not yet known",
          "probationOfficerContactNumber" to "Not yet known",
        )
      }
      with(emailCaptor.secondValue) {
        this isInstanceOf TransferredProbationBookingProbationEmail::class.java
        address isEqualTo PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "dateOfBirth" to LocalDate.EPOCH.toMediumFormatStyle(),
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "Not yet known",
          "probationOfficerEmailAddress" to "Not yet known",
          "probationOfficerContactNumber" to "Not yet known",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "prison template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "TRANSFERRED"
      }
      with(notificationCaptor.secondValue) {
        email isEqualTo PROBATION_USER.email
        templateName isEqualTo "probation template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "TRANSFERRED"
      }

      with(telemetryCaptor.firstValue as ProbationBookingCancelledTelemetryEvent) {
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
        firstName = "Bob",
        lastName = "Builder",
        dateOfBirth = LocalDate.EPOCH,
        lastPrisonId = WANDSWORTH,
      )

      setupCourtPrimaryContactsFor(SERVICE_USER)

      whenever(videoBookingServiceDelegate.cancel(1, SERVICE_USER)) doReturn courtBooking.apply { cancel(SERVICE_USER) }
      whenever(prisonerSearchClient.getPrisoner(courtBooking.prisoner())) doReturn prisoner
      whenever(emailService.send(any<ReleasedCourtBookingCourtEmail>())) doReturn Result.success(emailNotificationId to "court template id")
      whenever(emailService.send(any<ReleasedCourtBookingPrisonCourtEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.prisonerReleased(1, SERVICE_USER)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailService, notificationRepository, telemetryService) {
        verify(videoBookingServiceDelegate).cancel(1, SERVICE_USER)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, 1)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf ReleasedCourtBookingPrisonCourtEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Bob Builder",
          "dateOfBirth" to LocalDate.EPOCH.toMediumFormatStyle(),
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
        )
      }

      with(emailCaptor.secondValue) {
        this isInstanceOf ReleasedCourtBookingCourtEmail::class.java
        address isEqualTo COURT_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Bob Builder",
          "dateOfBirth" to LocalDate.EPOCH.toMediumFormatStyle(),
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "prison template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "RELEASED"
      }
      with(notificationCaptor.secondValue) {
        email isEqualTo COURT_USER.email
        templateName isEqualTo "court template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "RELEASED"
      }

      with(telemetryCaptor.firstValue as CourtBookingCancelledTelemetryEvent) {
        properties()["cancelled_by"] isEqualTo "release"
      }
    }

    @Test
    fun `should send events and emails on release of prisoner by service user for a probation booking`() {
      val prisoner = Prisoner(
        prisonerNumber = probationBookingAtBirminghamPrison.prisoner(),
        prisonId = "TRN",
        firstName = "Bob",
        lastName = "Builder",
        dateOfBirth = LocalDate.EPOCH,
        lastPrisonId = WANDSWORTH,
      )

      setupProbationPrimaryContacts(SERVICE_USER)

      whenever(videoBookingServiceDelegate.cancel(1, SERVICE_USER)) doReturn probationBookingAtBirminghamPrison.apply { cancel(SERVICE_USER) }
      whenever(prisonerSearchClient.getPrisoner(probationBookingAtBirminghamPrison.prisoner())) doReturn prisoner
      whenever(emailService.send(any<ReleasedProbationBookingProbationEmail>())) doReturn Result.success(emailNotificationId to "probation template id")
      whenever(emailService.send(any<ReleasedProbationBookingPrisonProbationEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.prisonerReleased(1, SERVICE_USER)

      inOrder(videoBookingServiceDelegate, outboundEventsService, emailService, notificationRepository, telemetryService) {
        verify(videoBookingServiceDelegate).cancel(1, SERVICE_USER)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, 1)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(telemetryService).track(telemetryCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf ReleasedProbationBookingPrisonProbationEmail::class.java
        address isEqualTo PRISON_USER_BIRMINGHAM.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "probationEmailAddress" to "probation.user@probation.com",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "dateOfBirth" to LocalDate.EPOCH.toMediumFormatStyle(),
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "Not yet known",
          "probationOfficerEmailAddress" to "Not yet known",
          "probationOfficerContactNumber" to "Not yet known",
        )
      }
      with(emailCaptor.secondValue) {
        this isInstanceOf ReleasedProbationBookingProbationEmail::class.java
        address isEqualTo PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "dateOfBirth" to LocalDate.EPOCH.toMediumFormatStyle(),
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "prisonVideoUrl" to "birmingham-video-url",
          "probationOfficerName" to "Not yet known",
          "probationOfficerEmailAddress" to "Not yet known",
          "probationOfficerContactNumber" to "Not yet known",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER_BIRMINGHAM.email
        templateName isEqualTo "prison template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "RELEASED"
      }
      with(notificationCaptor.secondValue) {
        email isEqualTo PROBATION_USER.email
        templateName isEqualTo "probation template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "RELEASED"
      }

      with(telemetryCaptor.firstValue as ProbationBookingCancelledTelemetryEvent) {
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
        firstName = "Bob",
        lastName = "Builder",
        dateOfBirth = LocalDate.EPOCH,
        lastPrisonId = WANDSWORTH,
      )

      setupCourtPrimaryContactsFor(SERVICE_USER)

      whenever(prisonerSearchClient.getPrisoner(courtBooking.prisoner())) doReturn prisoner
      whenever(emailService.send(any<CourtHearingLinkReminderEmail>())) doReturn Result.success(emailNotificationId to "court template id")

      facade.courtHearingLinkReminder(courtBooking, SERVICE_USER)

      inOrder(emailService, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 1
      with(emailCaptor.firstValue) {
        this isInstanceOf CourtHearingLinkReminderEmail::class.java
        address isEqualTo COURT_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Wandsworth",
          "offenderNo" to "123456",
          "prisonerName" to "Bob Builder",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${wandsworthLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "court notes for staff",
          "courtHearingLink" to "Not yet known",
          "bookingId" to "0",
        )
      }

      notificationCaptor.allValues hasSize 1
      with(notificationCaptor.firstValue) {
        email isEqualTo COURT_USER.email
        templateName isEqualTo "court template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "COURT_HEARING_LINK_REMINDER"
      }
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
        firstName = "Bob",
        lastName = "Builder",
        dateOfBirth = LocalDate.EPOCH,
        lastPrisonId = BIRMINGHAM,
      )

      setupCourtPrimaryContactsFor(SERVICE_USER)

      whenever(prisonerSearchClient.getPrisoner(probationBookingAtBirminghamPrison.prisoner())) doReturn prisoner
      whenever(emailService.send(any<ProbationOfficerDetailsReminderEmail>())) doReturn Result.success(emailNotificationId to "probation template id")

      facade.sendProbationOfficerDetailsReminder(probationBookingAtBirminghamPrison, SERVICE_USER)

      inOrder(emailService, notificationRepository) {
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 1
      with(emailCaptor.firstValue) {
        this isInstanceOf ProbationOfficerDetailsReminderEmail::class.java
        address isEqualTo PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "probationTeam" to "probation team description",
          "meetingType" to "PSR",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "probation notes for staff",
          "bookingId" to "0",
          "prisonVideoUrl" to "birmingham-video-url",
        )
      }

      notificationCaptor.allValues hasSize 1
      with(notificationCaptor.firstValue) {
        email isEqualTo PROBATION_USER.email
        templateName isEqualTo "probation template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "PROBATION_OFFICER_DETAILS_REMINDER"
      }
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

  private fun setupProbationPrimaryContacts(user: User) {
    val mayBeEmail = when (user) {
      is ExternalUser -> user.email
      is PrisonUser -> user.email
      else -> null
    }

    // Not ideal but have logic in test to mimic stubbed service behaviour regarding matching email addresses for contacts
    contactsService.stub {
      on { getBookingContacts(any(), eq(user)) } doReturn listOfNotNull(
        bookingContact(contactType = ContactType.USER, email = mayBeEmail, name = user.name).takeUnless { user is ServiceUser },
        bookingContact(contactType = ContactType.PRISON, email = PRISON_USER_BIRMINGHAM.email, name = PRISON_USER_BIRMINGHAM.name).takeUnless { it.email == mayBeEmail },
        bookingContact(contactType = ContactType.PROBATION, email = PROBATION_USER.email, name = PROBATION_USER.name).takeUnless { it.email == mayBeEmail },
      )
    }
  }

  private fun setupCourtPrimaryContactsFor(user: User) {
    val mayBeEmail = when (user) {
      is ExternalUser -> user.email
      is PrisonUser -> user.email
      else -> null
    }

    // Not ideal but have logic in test to mimic stubbed service behaviour regarding matching email addresses for contacts
    contactsService.stub {
      on { getBookingContacts(any(), eq(user)) } doReturn listOfNotNull(
        bookingContact(contactType = ContactType.USER, email = mayBeEmail, name = user.name).takeUnless { user is ServiceUser },
        bookingContact(contactType = ContactType.PRISON, email = PRISON_USER_BIRMINGHAM.email, name = PRISON_USER_BIRMINGHAM.name).takeUnless { it.email == mayBeEmail },
        bookingContact(contactType = ContactType.COURT, email = COURT_USER.email, name = COURT_USER.name).takeUnless { it.email == mayBeEmail },
        bookingContact(contactType = ContactType.PROBATION, email = PROBATION_USER.email, name = PROBATION_USER.name).takeUnless { it.email == mayBeEmail },
      )
    }
  }
}

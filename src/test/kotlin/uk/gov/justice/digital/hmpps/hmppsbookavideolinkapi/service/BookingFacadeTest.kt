package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toMediumFormatStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Email
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Notification
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PROBATION_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.SERVICE_USER
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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.yesterday
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.NewProbationBookingUserEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ReleasedProbationBookingPrisonProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.ReleasedProbationBookingProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.TransferredProbationBookingPrisonProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.TransferredProbationBookingProbationEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.OutboundEventsService
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class BookingFacadeTest {
  private val emailCaptor = argumentCaptor<Email>()
  private val createBookingService: CreateVideoBookingService = mock()
  private val amendBookingService: AmendVideoBookingService = mock()
  private val cancelVideoBookingService: CancelVideoBookingService = mock()
  private val contactsService: ContactsService = mock()
  private val prisonRepository: PrisonRepository = mock()
  private val emailService: EmailService = mock()
  private val notificationRepository: NotificationRepository = mock()
  private val outboundEventsService: OutboundEventsService = mock()
  private val notificationCaptor = argumentCaptor<Notification>()
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient = mock()
  private val prisonerSearchClient: PrisonerSearchClient = mock()
  private val facade = BookingFacade(
    createBookingService,
    amendBookingService,
    cancelVideoBookingService,
    contactsService,
    prisonRepository,
    emailService,
    notificationRepository,
    outboundEventsService,
    locationsInsidePrisonClient,
    prisonerSearchClient,
  )
  private val courtBooking = courtBooking()
    .addAppointment(
      prisonCode = MOORLAND,
      prisonerNumber = "123456",
      appointmentType = "VLB_COURT_MAIN",
      date = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      locationKey = moorlandLocation.key,
    )
  private val courtBookingAtDisabledCourt = courtBooking(court = court(enabled = false))
    .addAppointment(
      prisonCode = MOORLAND,
      prisonerNumber = "123456",
      appointmentType = "VLB_COURT_MAIN",
      date = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      locationKey = moorlandLocation.key,
    )
  private val courtBookingInThePast = courtBooking()
    .addAppointment(
      prisonCode = MOORLAND,
      prisonerNumber = "123456",
      appointmentType = "VLB_COURT_MAIN",
      date = LocalDate.now().minusDays(1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      locationKey = moorlandLocation.key,
    )
  private val courtBookingCreatedByPrison = courtBooking(createdByPrison = true)
    .addAppointment(
      prisonCode = MOORLAND,
      prisonerNumber = "123456",
      appointmentType = "VLB_COURT_MAIN",
      date = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      locationKey = moorlandLocation.key,
    )

  private val probationBookingAtBirminghamPrison = probationBooking()
    .addAppointment(
      prisonCode = BIRMINGHAM,
      prisonerNumber = "654321",
      appointmentType = AppointmentType.VLB_PROBATION.name,
      locationKey = birminghamLocation.key,
      date = tomorrow(),
      startTime = LocalTime.MIDNIGHT,
      endTime = LocalTime.MIDNIGHT.plusHours(1),
    )

  private val emailNotificationId = UUID.randomUUID()

  @BeforeEach
  fun before() {
    whenever(prisonRepository.findByCode(MOORLAND)) doReturn prison(MOORLAND)
    whenever(prisonRepository.findByCode(BIRMINGHAM)) doReturn prison(BIRMINGHAM)
    whenever(locationsInsidePrisonClient.getLocationsByKeys(setOf(moorlandLocation.key))) doReturn listOf(moorlandLocation)
    whenever(locationsInsidePrisonClient.getLocationByKey(birminghamLocation.key)) doReturn birminghamLocation
  }

  @Nested
  @DisplayName("Create bookings")
  inner class CreateBooking {
    @Test
    fun `should send events and emails on creation of court booking by court user`() {
      setupCourtPrimaryContactsFor(COURT_USER)

      val bookingRequest = courtBookingRequest(prisonCode = MOORLAND, prisonerNumber = courtBooking.prisoner())

      whenever(createBookingService.create(bookingRequest, COURT_USER)) doReturn Pair(courtBooking, prisoner(prisonerNumber = courtBooking.prisoner(), prisonCode = MOORLAND))
      whenever(emailService.send(any<NewCourtBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<NewCourtBookingPrisonNoCourtEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.create(bookingRequest, COURT_USER)

      inOrder(createBookingService, outboundEventsService, emailService, notificationRepository) {
        verify(createBookingService).create(bookingRequest, COURT_USER)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CREATED, courtBooking.videoBookingId)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf NewCourtBookingUserEmail::class.java
        address isEqualTo COURT_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to COURT_USER.name,
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Moorland",
          "offenderNo" to "123456",
          "prisonerName" to "Fred Bloggs",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${moorlandLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "Court hearing comments",
        )
      }
      with(emailCaptor.secondValue) {
        this isInstanceOf NewCourtBookingPrisonNoCourtEmail::class.java
        address isEqualTo PRISON_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Moorland",
          "offenderNo" to "123456",
          "prisonerName" to "Fred Bloggs",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${moorlandLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "Court hearing comments",
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
        email isEqualTo PRISON_USER.email
        templateName isEqualTo "prison template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "CREATE"
      }
    }

    @Test
    fun `should send events and emails on creation of court booking by a prison user`() {
      setupCourtPrimaryContactsFor(PRISON_USER)

      val bookingRequest = courtBookingRequest(prisonCode = MOORLAND, prisonerNumber = courtBookingCreatedByPrison.prisoner())

      whenever(createBookingService.create(bookingRequest, PRISON_USER)) doReturn Pair(courtBookingCreatedByPrison, prisoner(prisonerNumber = courtBookingCreatedByPrison.prisoner(), prisonCode = MOORLAND))
      whenever(emailService.send(any<NewCourtBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<NewCourtBookingCourtEmail>())) doReturn Result.success(emailNotificationId to "court template id")

      facade.create(bookingRequest, PRISON_USER)

      inOrder(createBookingService, outboundEventsService, emailService, notificationRepository) {
        verify(createBookingService).create(bookingRequest, PRISON_USER)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CREATED, courtBookingCreatedByPrison.videoBookingId)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf NewCourtBookingUserEmail::class.java
        address isEqualTo PRISON_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to PRISON_USER.name,
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Moorland",
          "offenderNo" to "123456",
          "prisonerName" to "Fred Bloggs",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${moorlandLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "Court hearing comments",
        )
      }
      with(emailCaptor.secondValue) {
        this isInstanceOf NewCourtBookingCourtEmail::class.java
        address isEqualTo COURT_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Moorland",
          "offenderNo" to "123456",
          "prisonerName" to "Fred Bloggs",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${moorlandLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "Court hearing comments",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER.email
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

      whenever(createBookingService.create(request, PROBATION_USER)) doReturn Pair(probationBookingAtBirminghamPrison, prisoner(prisonCode = prisoner.prisonId!!, prisonerNumber = prisoner.prisonerNumber, firstName = prisoner.firstName, lastName = prisoner.lastName))
      whenever(emailService.send(any<NewProbationBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<NewProbationBookingPrisonNoProbationEmail>())) doReturn Result.success(emailNotificationId to "probation template id")

      facade.create(request, PROBATION_USER)

      inOrder(createBookingService, outboundEventsService, emailService, notificationRepository) {
        verify(createBookingService).create(request, PROBATION_USER)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CREATED, probationBookingAtBirminghamPrison.videoBookingId)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
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
          "comments" to "Probation meeting comments",
        )
      }

      with(emailCaptor.secondValue) {
        this isInstanceOf NewProbationBookingPrisonNoProbationEmail::class.java
        address isEqualTo PRISON_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "Probation meeting comments",
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
        email isEqualTo PRISON_USER.email
        templateName isEqualTo "probation template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "CREATE"
      }
    }
  }

  @Nested
  @DisplayName("Cancel bookings")
  inner class CancelBooking {
    @Test
    fun `should send events and emails on cancellation of court booking by court user`() {
      setupCourtPrimaryContactsFor(COURT_USER)

      val prisoner = Prisoner(courtBooking.prisoner(), MOORLAND, "Bob", "Builder", yesterday())

      whenever(cancelVideoBookingService.cancel(1, COURT_USER)) doReturn courtBooking.apply { cancel(COURT_USER) }
      whenever(prisonerSearchClient.getPrisoner(courtBooking.prisoner())) doReturn prisoner
      whenever(emailService.send(any<CancelledCourtBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<CancelledCourtBookingPrisonNoCourtEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.cancel(1, COURT_USER)

      inOrder(cancelVideoBookingService, outboundEventsService, emailService, notificationRepository) {
        verify(cancelVideoBookingService).cancel(1, COURT_USER)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, 1)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf CancelledCourtBookingUserEmail::class.java
        address isEqualTo COURT_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to COURT_USER.name,
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Moorland",
          "offenderNo" to "123456",
          "prisonerName" to "Bob Builder",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${moorlandLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "Court hearing comments",
        )
      }
      with(emailCaptor.secondValue) {
        this isInstanceOf CancelledCourtBookingPrisonNoCourtEmail::class.java
        address isEqualTo PRISON_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Moorland",
          "offenderNo" to "123456",
          "prisonerName" to "Bob Builder",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${moorlandLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "Court hearing comments",
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
        email isEqualTo PRISON_USER.email
        templateName isEqualTo "prison template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "CANCEL"
      }
    }

    @Test
    fun `should send events and emails on cancellation of court booking by prison user`() {
      setupCourtPrimaryContactsFor(PRISON_USER)

      val prisoner = Prisoner(courtBooking.prisoner(), MOORLAND, "Bob", "Builder", yesterday())

      whenever(cancelVideoBookingService.cancel(1, PRISON_USER)) doReturn courtBooking.apply { cancel(PRISON_USER) }
      whenever(prisonerSearchClient.getPrisoner(courtBooking.prisoner())) doReturn prisoner
      whenever(emailService.send(any<CancelledCourtBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<CancelledCourtBookingCourtEmail>())) doReturn Result.success(emailNotificationId to "court template id")

      facade.cancel(1, PRISON_USER)

      inOrder(cancelVideoBookingService, outboundEventsService, emailService, notificationRepository) {
        verify(cancelVideoBookingService).cancel(1, PRISON_USER)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, 1)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf CancelledCourtBookingUserEmail::class.java
        address isEqualTo PRISON_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to PRISON_USER.name,
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Moorland",
          "offenderNo" to "123456",
          "prisonerName" to "Bob Builder",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${moorlandLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "Court hearing comments",
        )
      }
      with(emailCaptor.secondValue) {
        this isInstanceOf CancelledCourtBookingCourtEmail::class.java
        address isEqualTo COURT_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Moorland",
          "offenderNo" to "123456",
          "prisonerName" to "Bob Builder",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${moorlandLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "Court hearing comments",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER.email
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
    }

    @Test
    fun `should send events and emails on cancellation of probation booking by probation user`() {
      setupProbationPrimaryContacts(PROBATION_USER)

      val prisoner = Prisoner(probationBookingAtBirminghamPrison.prisoner(), BIRMINGHAM, "Bob", "Builder", yesterday())

      whenever(prisonerSearchClient.getPrisoner(prisoner.prisonerNumber)) doReturn prisoner
      whenever(cancelVideoBookingService.cancel(1, PROBATION_USER)) doReturn probationBookingAtBirminghamPrison.apply { cancel(PROBATION_USER) }
      whenever(emailService.send(any<CancelledProbationBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<CancelledProbationBookingPrisonNoProbationEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.cancel(1, PROBATION_USER)

      inOrder(cancelVideoBookingService, outboundEventsService, emailService, notificationRepository) {
        verify(cancelVideoBookingService).cancel(1, PROBATION_USER)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, 1)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
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
          "comments" to "Probation meeting comments",
        )
      }

      with(emailCaptor.secondValue) {
        this isInstanceOf CancelledProbationBookingPrisonNoProbationEmail::class.java
        address isEqualTo PRISON_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "Probation meeting comments",
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
        email isEqualTo PRISON_USER.email
        templateName isEqualTo "prison template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "CANCEL"
      }
    }

    @Test
    fun `should send events and emails on cancellation of probation booking by prison user`() {
      setupProbationPrimaryContacts(PRISON_USER)

      val prisoner = Prisoner(probationBookingAtBirminghamPrison.prisoner(), BIRMINGHAM, "Bob", "Builder", yesterday())

      whenever(prisonerSearchClient.getPrisoner(prisoner.prisonerNumber)) doReturn prisoner
      whenever(cancelVideoBookingService.cancel(1, PRISON_USER)) doReturn probationBookingAtBirminghamPrison.apply { cancel(PRISON_USER) }
      whenever(emailService.send(any<CancelledProbationBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<CancelledProbationBookingProbationEmail>())) doReturn Result.success(emailNotificationId to "probation template id")

      facade.cancel(1, PRISON_USER)

      inOrder(cancelVideoBookingService, outboundEventsService, emailService, notificationRepository) {
        verify(cancelVideoBookingService).cancel(1, PRISON_USER)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, 1)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf CancelledProbationBookingUserEmail::class.java
        address isEqualTo PRISON_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to PRISON_USER.name,
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "Probation meeting comments",
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
          "comments" to "Probation meeting comments",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER.email
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
    }
  }

  @Nested
  @DisplayName("Amend bookings")
  inner class AmendBooking {
    @Test
    fun `should send events and emails on amendment of court booking by court user`() {
      setupCourtPrimaryContactsFor(COURT_USER)

      val bookingRequest = amendCourtBookingRequest(prisonCode = MOORLAND, prisonerNumber = "123456")

      whenever(amendBookingService.amend(1, bookingRequest, COURT_USER)) doReturn Pair(courtBooking, prisoner(prisonerNumber = "123456", prisonCode = MOORLAND))
      whenever(emailService.send(any<AmendedCourtBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<AmendedCourtBookingPrisonNoCourtEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.amend(1, bookingRequest, COURT_USER)

      inOrder(amendBookingService, emailService, notificationRepository) {
        verify(amendBookingService).amend(1, bookingRequest, COURT_USER)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
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
          "mainAppointmentInfo" to "${moorlandLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "Court hearing comments",
          "prison" to "Moorland",
        )
      }
      with(emailCaptor.secondValue) {
        this isInstanceOf AmendedCourtBookingPrisonNoCourtEmail::class.java
        address isEqualTo PRISON_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Moorland",
          "offenderNo" to "123456",
          "prisonerName" to "Fred Bloggs",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${moorlandLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "Court hearing comments",
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
        email isEqualTo PRISON_USER.email
        templateName isEqualTo "prison template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo courtBooking
        reason isEqualTo "AMEND"
      }
    }

    @Test
    fun `should send events and emails on amendment of court booking by prison user`() {
      setupCourtPrimaryContactsFor(PRISON_USER)

      val bookingRequest = amendCourtBookingRequest(prisonCode = MOORLAND, prisonerNumber = "123456")

      whenever(amendBookingService.amend(1, bookingRequest, PRISON_USER)) doReturn Pair(courtBooking, prisoner(prisonerNumber = "123456", prisonCode = MOORLAND))
      whenever(emailService.send(any<AmendedCourtBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<AmendedCourtBookingCourtEmail>())) doReturn Result.success(emailNotificationId to "court template id")

      facade.amend(1, bookingRequest, PRISON_USER)

      inOrder(amendBookingService, emailService, notificationRepository) {
        verify(amendBookingService).amend(1, bookingRequest, PRISON_USER)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf AmendedCourtBookingUserEmail::class.java
        address isEqualTo PRISON_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to PRISON_USER.name,
          "court" to DERBY_JUSTICE_CENTRE,
          "offenderNo" to "123456",
          "prisonerName" to "Fred Bloggs",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${moorlandLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "Court hearing comments",
          "prison" to "Moorland",
        )
      }
      with(emailCaptor.secondValue) {
        this isInstanceOf AmendedCourtBookingCourtEmail::class.java
        address isEqualTo COURT_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Moorland",
          "offenderNo" to "123456",
          "prisonerName" to "Fred Bloggs",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${moorlandLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "Court hearing comments",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER.email
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
    }

    @Test
    fun `should send events and emails on amendment of probation booking by probation user`() {
      setupProbationPrimaryContacts(PROBATION_USER)

      val prisoner = Prisoner(probationBookingAtBirminghamPrison.prisoner(), BIRMINGHAM, "Bob", "Builder", yesterday())

      whenever(prisonerSearchClient.getPrisoner(prisoner.prisonerNumber)) doReturn prisoner

      val amendRequest = amendProbationBookingRequest(prisonCode = prisoner.prisonId!!, prisonerNumber = prisoner.prisonerNumber, appointmentDate = tomorrow())

      whenever(amendBookingService.amend(1, amendRequest, PROBATION_USER)) doReturn Pair(probationBookingAtBirminghamPrison, prisoner(prisonerNumber = prisoner.prisonerNumber, prisonCode = prisoner.prisonId!!))
      whenever(emailService.send(any<AmendedProbationBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<AmendedProbationBookingPrisonNoProbationEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.amend(1, amendRequest, PROBATION_USER)

      inOrder(amendBookingService, outboundEventsService, emailService, notificationRepository) {
        verify(amendBookingService).amend(1, amendRequest, PROBATION_USER)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_AMENDED, 1)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
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
          "comments" to "Probation meeting comments",
        )
      }
      with(emailCaptor.secondValue) {
        this isInstanceOf AmendedProbationBookingPrisonNoProbationEmail::class.java
        address isEqualTo PRISON_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Fred Bloggs",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "Probation meeting comments",
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
        email isEqualTo PRISON_USER.email
        templateName isEqualTo "prison template id"
        govNotifyNotificationId isEqualTo emailNotificationId
        videoBooking isEqualTo probationBookingAtBirminghamPrison
        reason isEqualTo "AMEND"
      }
    }

    @Test
    fun `should send events and emails on amendment of probation booking by prison user`() {
      setupProbationPrimaryContacts(PRISON_USER)

      val prisoner = Prisoner(probationBookingAtBirminghamPrison.prisoner(), BIRMINGHAM, "Bob", "Builder", yesterday())

      whenever(prisonerSearchClient.getPrisoner(prisoner.prisonerNumber)) doReturn prisoner

      val amendRequest = amendProbationBookingRequest(prisonCode = prisoner.prisonId!!, prisonerNumber = prisoner.prisonerNumber, appointmentDate = tomorrow())

      whenever(amendBookingService.amend(1, amendRequest, PRISON_USER)) doReturn Pair(probationBookingAtBirminghamPrison, prisoner(prisonerNumber = prisoner.prisonerNumber, prisonCode = prisoner.prisonId!!))
      whenever(emailService.send(any<AmendedProbationBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
      whenever(emailService.send(any<AmendedProbationBookingProbationEmail>())) doReturn Result.success(emailNotificationId to "probation template id")

      facade.amend(1, amendRequest, PRISON_USER)

      inOrder(amendBookingService, outboundEventsService, emailService, notificationRepository) {
        verify(amendBookingService).amend(1, amendRequest, PRISON_USER)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_AMENDED, 1)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf AmendedProbationBookingUserEmail::class.java
        address isEqualTo PRISON_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "userName" to PRISON_USER.name,
          "probationTeam" to "probation team description",
          "prison" to "Birmingham",
          "offenderNo" to "654321",
          "prisonerName" to "Fred Bloggs",
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "Probation meeting comments",
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
          "comments" to "Probation meeting comments",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER.email
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
        lastPrisonId = MOORLAND,
      )
      whenever(contactsService.getPrimaryBookingContacts(any(), anyOrNull())) doReturn listOf(
        bookingContact(contactType = ContactType.PRISON, email = "jon@prison.com", name = "Jon"),
      )

      whenever(cancelVideoBookingService.cancel(1, SERVICE_USER)) doReturn courtBooking.apply { cancel(SERVICE_USER) }
      whenever(prisonerSearchClient.getPrisoner(courtBooking.prisoner())) doReturn prisoner
      whenever(emailService.send(any<TransferredCourtBookingPrisonNoCourtEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.prisonerTransferred(1, SERVICE_USER)

      inOrder(cancelVideoBookingService, outboundEventsService, emailService, notificationRepository) {
        verify(cancelVideoBookingService).cancel(1, SERVICE_USER)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, 1)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 1
      with(emailCaptor.firstValue) {
        this isInstanceOf TransferredCourtBookingPrisonNoCourtEmail::class.java
        address isEqualTo "jon@prison.com"
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Moorland",
          "offenderNo" to "123456",
          "prisonerName" to "Bob Builder",
          "dateOfBirth" to LocalDate.EPOCH.toMediumFormatStyle(),
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${moorlandLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "Court hearing comments",
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
    }

    @Test
    fun `should send events and emails on release of prisoner by service user for a probation booking`() {
      val prisoner = Prisoner(
        prisonerNumber = probationBookingAtBirminghamPrison.prisoner(),
        prisonId = "TRN",
        firstName = "Bob",
        lastName = "Builder",
        dateOfBirth = LocalDate.EPOCH,
        lastPrisonId = MOORLAND,
      )

      setupProbationPrimaryContacts(SERVICE_USER)

      whenever(cancelVideoBookingService.cancel(1, SERVICE_USER)) doReturn probationBookingAtBirminghamPrison.apply { cancel(SERVICE_USER) }
      whenever(prisonerSearchClient.getPrisoner(probationBookingAtBirminghamPrison.prisoner())) doReturn prisoner
      whenever(emailService.send(any<TransferredProbationBookingProbationEmail>())) doReturn Result.success(emailNotificationId to "probation template id")
      whenever(emailService.send(any<TransferredProbationBookingPrisonProbationEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.prisonerTransferred(1, SERVICE_USER)

      inOrder(cancelVideoBookingService, outboundEventsService, emailService, notificationRepository) {
        verify(cancelVideoBookingService).cancel(1, SERVICE_USER)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, 1)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf TransferredProbationBookingPrisonProbationEmail::class.java
        address isEqualTo PRISON_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "probationEmailAddress" to "probation.user@probation.com",
          "prison" to "Moorland",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "dateOfBirth" to LocalDate.EPOCH.toMediumFormatStyle(),
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "Probation meeting comments",
        )
      }
      with(emailCaptor.secondValue) {
        this isInstanceOf TransferredProbationBookingProbationEmail::class.java
        address isEqualTo PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Moorland",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "dateOfBirth" to LocalDate.EPOCH.toMediumFormatStyle(),
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "Probation meeting comments",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER.email
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
        lastPrisonId = MOORLAND,
      )

      setupCourtPrimaryContactsFor(SERVICE_USER)

      whenever(cancelVideoBookingService.cancel(1, SERVICE_USER)) doReturn courtBooking.apply { cancel(SERVICE_USER) }
      whenever(prisonerSearchClient.getPrisoner(courtBooking.prisoner())) doReturn prisoner
      whenever(emailService.send(any<ReleasedCourtBookingCourtEmail>())) doReturn Result.success(emailNotificationId to "court template id")
      whenever(emailService.send(any<ReleasedCourtBookingPrisonCourtEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.prisonerReleased(1, SERVICE_USER)

      inOrder(cancelVideoBookingService, outboundEventsService, emailService, notificationRepository) {
        verify(cancelVideoBookingService).cancel(1, SERVICE_USER)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, 1)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf ReleasedCourtBookingPrisonCourtEmail::class.java
        address isEqualTo PRISON_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Moorland",
          "offenderNo" to "123456",
          "prisonerName" to "Bob Builder",
          "dateOfBirth" to LocalDate.EPOCH.toMediumFormatStyle(),
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${moorlandLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "Court hearing comments",
        )
      }

      with(emailCaptor.secondValue) {
        this isInstanceOf ReleasedCourtBookingCourtEmail::class.java
        address isEqualTo COURT_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "court" to DERBY_JUSTICE_CENTRE,
          "prison" to "Moorland",
          "offenderNo" to "123456",
          "prisonerName" to "Bob Builder",
          "dateOfBirth" to LocalDate.EPOCH.toMediumFormatStyle(),
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${moorlandLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "Court hearing comments",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER.email
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
    }

    @Test
    fun `should send events and emails on release of prisoner by service user for a probation booking`() {
      val prisoner = Prisoner(
        prisonerNumber = probationBookingAtBirminghamPrison.prisoner(),
        prisonId = "TRN",
        firstName = "Bob",
        lastName = "Builder",
        dateOfBirth = LocalDate.EPOCH,
        lastPrisonId = MOORLAND,
      )

      setupProbationPrimaryContacts(SERVICE_USER)

      whenever(cancelVideoBookingService.cancel(1, SERVICE_USER)) doReturn probationBookingAtBirminghamPrison.apply { cancel(SERVICE_USER) }
      whenever(prisonerSearchClient.getPrisoner(probationBookingAtBirminghamPrison.prisoner())) doReturn prisoner
      whenever(emailService.send(any<ReleasedProbationBookingProbationEmail>())) doReturn Result.success(emailNotificationId to "probation template id")
      whenever(emailService.send(any<ReleasedProbationBookingPrisonProbationEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

      facade.prisonerReleased(1, SERVICE_USER)

      inOrder(cancelVideoBookingService, outboundEventsService, emailService, notificationRepository) {
        verify(cancelVideoBookingService).cancel(1, SERVICE_USER)
        verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, 1)
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
        verify(emailService).send(emailCaptor.capture())
        verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      }

      emailCaptor.allValues hasSize 2
      with(emailCaptor.firstValue) {
        this isInstanceOf ReleasedProbationBookingPrisonProbationEmail::class.java
        address isEqualTo PRISON_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "probationEmailAddress" to "probation.user@probation.com",
          "prison" to "Moorland",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "dateOfBirth" to LocalDate.EPOCH.toMediumFormatStyle(),
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "Probation meeting comments",
        )
      }
      with(emailCaptor.secondValue) {
        this isInstanceOf ReleasedProbationBookingProbationEmail::class.java
        address isEqualTo PROBATION_USER.email
        personalisation() containsEntriesExactlyInAnyOrder mapOf(
          "probationTeam" to "probation team description",
          "prison" to "Moorland",
          "offenderNo" to "654321",
          "prisonerName" to "Bob Builder",
          "dateOfBirth" to LocalDate.EPOCH.toMediumFormatStyle(),
          "date" to tomorrow().toMediumFormatStyle(),
          "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
          "comments" to "Probation meeting comments",
        )
      }

      notificationCaptor.allValues hasSize 2
      with(notificationCaptor.firstValue) {
        email isEqualTo PRISON_USER.email
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
    }
  }

  @Nested
  @DisplayName("Court hearing link reminder")
  inner class CourtHearingLinkReminder {
    @Test
    fun `should send an email to the court contact to remind them to add a court hearing link`() {
      val prisoner = Prisoner(
        prisonerNumber = courtBooking.prisoner(),
        prisonId = "MDI",
        firstName = "Bob",
        lastName = "Builder",
        dateOfBirth = LocalDate.EPOCH,
        lastPrisonId = MOORLAND,
      )

      setupCourtPrimaryContactsFor(SERVICE_USER)

      whenever(prisonerSearchClient.getPrisoner(courtBooking.prisoner())) doReturn prisoner
      whenever(emailService.send(any<CourtHearingLinkReminderEmail>())) doReturn Result.success(emailNotificationId to "court template id")

      facade.courtHearingLinkReminder(courtBooking.apply { videoUrl = null }, SERVICE_USER)

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
          "prison" to "Moorland",
          "offenderNo" to "123456",
          "prisonerName" to "Bob Builder",
          "date" to "1 Jan 2100",
          "preAppointmentInfo" to "Not required",
          "mainAppointmentInfo" to "${moorlandLocation.localName} - 11:00 to 11:30",
          "postAppointmentInfo" to "Not required",
          "comments" to "Court hearing comments",
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
      error.message isEqualTo "Video booking with id 0 is not a court booking"
    }

    @Test
    fun `should throw an exception if the court is not enabled`() {
      val error = assertThrows<IllegalArgumentException> { facade.courtHearingLinkReminder(courtBookingAtDisabledCourt, SERVICE_USER) }
      error.message isEqualTo "Video booking with id 0 is not with an enabled court"
    }

    @Test
    fun `should throw an exception if booking has already taken place`() {
      val error = assertThrows<IllegalArgumentException> { facade.courtHearingLinkReminder(courtBookingInThePast, SERVICE_USER) }
      error.message isEqualTo "Video booking with id 0 has already taken place"
    }

    @Test
    fun `should throw an exception if booking already has a court hearing link`() {
      val error = assertThrows<IllegalArgumentException> { facade.courtHearingLinkReminder(courtBooking, SERVICE_USER) }
      error.message isEqualTo "Video booking with id 0 already has a court hearing link"
    }
  }

  private fun setupProbationPrimaryContacts(user: User) {
    // Not ideal but have logic in test to mimic stubbed service behaviour regarding matching email addresses for contacts
    contactsService.stub {
      on { getPrimaryBookingContacts(any(), eq(user)) } doReturn listOfNotNull(
        bookingContact(contactType = ContactType.USER, email = user.email, name = user.name).takeUnless { user.isUserType(UserType.SERVICE) },
        bookingContact(contactType = ContactType.PRISON, email = PRISON_USER.email, name = PRISON_USER.name).takeUnless { it.email == user.email },
        bookingContact(contactType = ContactType.PROBATION, email = PROBATION_USER.email, name = PROBATION_USER.name).takeUnless { it.email == user.email },
      )
    }
  }

  private fun setupCourtPrimaryContactsFor(user: User) {
    // Not ideal but have logic in test to mimic stubbed service behaviour regarding matching email addresses for contacts
    contactsService.stub {
      on { getPrimaryBookingContacts(any(), eq(user)) } doReturn listOfNotNull(
        bookingContact(contactType = ContactType.USER, email = user.email, name = user.name).takeUnless { user.isUserType(UserType.SERVICE) },
        bookingContact(contactType = ContactType.PRISON, email = PRISON_USER.email, name = PRISON_USER.name).takeUnless { it.email == user.email },
        bookingContact(contactType = ContactType.COURT, email = COURT_USER.email, name = COURT_USER.name).takeUnless { it.email == user.email },
      )
    }
  }
}

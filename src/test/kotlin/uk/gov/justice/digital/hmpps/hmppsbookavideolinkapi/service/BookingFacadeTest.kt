package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendCourtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendProbationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.bookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsEntriesExactlyInAnyOrder
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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.user
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.yesterday
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AppointmentType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.OutboundEventsService
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

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
    whenever(contactsService.getPrimaryBookingContacts(any(), any())) doReturn listOf(
      bookingContact(contactType = ContactType.USER, email = "jon@somewhere.com", name = "Jon"),
      bookingContact(contactType = ContactType.COURT, email = "jon@court.com", name = "Jon"),
      bookingContact(contactType = ContactType.PRISON, email = "jon@prison.com", name = "Jon"),
      bookingContact(contactType = ContactType.PROBATION, email = "jon@probation.com", name = "Jon"),
    )
  }

  @Test
  fun `should send two cancellation court booking emails and booking cancellation event on cancellation of court booking`() {
    val prisoner = Prisoner(courtBooking.prisoner(), MOORLAND, "Bob", "Builder", yesterday())

    whenever(cancelVideoBookingService.cancel(1, user("facade court user"))) doReturn courtBooking
    whenever(prisonerSearchClient.getPrisoner(courtBooking.prisoner())) doReturn prisoner

    whenever(emailService.send(any<CancelledCourtBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
    whenever(emailService.send(any<CancelledCourtBookingPrisonCourtEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

    facade.cancel(1, user("facade court user"))

    inOrder(cancelVideoBookingService, outboundEventsService, emailService, notificationRepository) {
      verify(cancelVideoBookingService).cancel(1, user("facade court user"))
      verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, 1)
      verify(emailService).send(emailCaptor.capture())
      verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      verify(emailService).send(emailCaptor.capture())
      verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
    }

    emailCaptor.allValues hasSize 2
    with(emailCaptor.firstValue) {
      this isInstanceOf CancelledCourtBookingUserEmail::class.java
      address isEqualTo "jon@somewhere.com"
      personalisation() containsEntriesExactlyInAnyOrder mapOf(
        "userName" to "Jon",
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
      this isInstanceOf CancelledCourtBookingPrisonCourtEmail::class.java
      address isEqualTo "jon@prison.com"
      personalisation() containsEntriesExactlyInAnyOrder mapOf(
        "court" to DERBY_JUSTICE_CENTRE,
        "courtEmailAddress" to "jon@court.com",
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
      email isEqualTo "jon@somewhere.com"
      templateName isEqualTo "user template id"
      govNotifyNotificationId isEqualTo emailNotificationId
      videoBooking isEqualTo courtBooking
      reason isEqualTo "Cancelled court booking"
    }
    with(notificationCaptor.secondValue) {
      email isEqualTo "jon@prison.com"
      templateName isEqualTo "prison template id"
      govNotifyNotificationId isEqualTo emailNotificationId
      videoBooking isEqualTo courtBooking
      reason isEqualTo "Cancelled court booking"
    }
  }

  @Test
  fun `should send two court booking emails and booking created event on creation of court booking`() {
    val bookingRequest = courtBookingRequest(prisonCode = MOORLAND, prisonerNumber = courtBooking.prisoner())

    whenever(createBookingService.create(bookingRequest, user("facade court user"))) doReturn Pair(courtBooking, prisoner(prisonerNumber = courtBooking.prisoner(), prisonCode = MOORLAND))
    whenever(emailService.send(any<NewCourtBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
    whenever(emailService.send(any<NewCourtBookingPrisonCourtEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

    facade.create(bookingRequest, user("facade court user"))

    inOrder(createBookingService, outboundEventsService, emailService, notificationRepository) {
      verify(createBookingService).create(bookingRequest, user("facade court user"))
      verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CREATED, courtBooking.videoBookingId)
      verify(emailService).send(emailCaptor.capture())
      verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      verify(emailService).send(emailCaptor.capture())
      verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
    }

    emailCaptor.allValues hasSize 2
    with(emailCaptor.firstValue) {
      this isInstanceOf NewCourtBookingUserEmail::class.java
      address isEqualTo "jon@somewhere.com"
      personalisation() containsEntriesExactlyInAnyOrder mapOf(
        "userName" to "Jon",
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
      this isInstanceOf NewCourtBookingPrisonCourtEmail::class.java
      address isEqualTo "jon@prison.com"
      personalisation() containsEntriesExactlyInAnyOrder mapOf(
        "court" to DERBY_JUSTICE_CENTRE,
        "courtEmailAddress" to "jon@court.com",
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
      email isEqualTo "jon@somewhere.com"
      templateName isEqualTo "user template id"
      govNotifyNotificationId isEqualTo emailNotificationId
      videoBooking isEqualTo courtBooking
      reason isEqualTo "New court booking"
    }
    with(notificationCaptor.secondValue) {
      email isEqualTo "jon@prison.com"
      templateName isEqualTo "prison template id"
      govNotifyNotificationId isEqualTo emailNotificationId
      videoBooking isEqualTo courtBooking
      reason isEqualTo "New court booking"
    }
  }

  @Test
  fun `should send three court booking emails and booking created event on creation of court booking by a prison`() {
    val user = user("facade prison user", userType = UserType.PRISON)
    val bookingRequest = courtBookingRequest(prisonCode = MOORLAND, prisonerNumber = courtBookingCreatedByPrison.prisoner())

    whenever(createBookingService.create(bookingRequest, user)) doReturn Pair(courtBookingCreatedByPrison, prisoner(prisonerNumber = courtBookingCreatedByPrison.prisoner(), prisonCode = MOORLAND))
    whenever(emailService.send(any<NewCourtBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
    whenever(emailService.send(any<NewCourtBookingCourtEmail>())) doReturn Result.success(emailNotificationId to "court template id")
    whenever(emailService.send(any<NewCourtBookingPrisonCourtEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

    facade.create(bookingRequest, user)

    inOrder(createBookingService, outboundEventsService, emailService, notificationRepository) {
      verify(createBookingService).create(bookingRequest, user)
      verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CREATED, courtBookingCreatedByPrison.videoBookingId)
      verify(emailService).send(emailCaptor.capture())
      verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      verify(emailService).send(emailCaptor.capture())
      verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      verify(emailService).send(emailCaptor.capture())
      verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
    }

    emailCaptor.allValues hasSize 3
    with(emailCaptor.firstValue) {
      this isInstanceOf NewCourtBookingUserEmail::class.java
      address isEqualTo "jon@somewhere.com"
      personalisation() containsEntriesExactlyInAnyOrder mapOf(
        "userName" to "Jon",
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
      address isEqualTo "jon@court.com"
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
    with(emailCaptor.thirdValue) {
      this isInstanceOf NewCourtBookingPrisonCourtEmail::class.java
      address isEqualTo "jon@prison.com"
      personalisation() containsEntriesExactlyInAnyOrder mapOf(
        "court" to DERBY_JUSTICE_CENTRE,
        "courtEmailAddress" to "jon@court.com",
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

    notificationCaptor.allValues hasSize 3
    with(notificationCaptor.firstValue) {
      email isEqualTo "jon@somewhere.com"
      templateName isEqualTo "user template id"
      govNotifyNotificationId isEqualTo emailNotificationId
      videoBooking isEqualTo courtBookingCreatedByPrison
      reason isEqualTo "New court booking"
    }
    with(notificationCaptor.secondValue) {
      email isEqualTo "jon@court.com"
      templateName isEqualTo "court template id"
      govNotifyNotificationId isEqualTo emailNotificationId
      videoBooking isEqualTo courtBookingCreatedByPrison
      reason isEqualTo "New court booking"
    }
    with(notificationCaptor.thirdValue) {
      email isEqualTo "jon@prison.com"
      templateName isEqualTo "prison template id"
      govNotifyNotificationId isEqualTo emailNotificationId
      videoBooking isEqualTo courtBookingCreatedByPrison
      reason isEqualTo "New court booking"
    }
  }

  @Test
  fun `should send two court booking emails on amendment of court booking`() {
    val bookingRequest = amendCourtBookingRequest(prisonCode = MOORLAND, prisonerNumber = "123456")

    whenever(amendBookingService.amend(1, bookingRequest, user("facade court user"))) doReturn Pair(courtBooking, prisoner(prisonerNumber = "123456", prisonCode = MOORLAND))
    whenever(emailService.send(any<AmendedCourtBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
    whenever(emailService.send(any<AmendedCourtBookingPrisonCourtEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

    facade.amend(1, bookingRequest, user("facade court user"))

    inOrder(amendBookingService, emailService, notificationRepository) {
      verify(amendBookingService).amend(1, bookingRequest, user("facade court user"))
      verify(emailService).send(emailCaptor.capture())
      verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      verify(emailService).send(emailCaptor.capture())
      verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
    }

    emailCaptor.allValues hasSize 2
    with(emailCaptor.firstValue) {
      this isInstanceOf AmendedCourtBookingUserEmail::class.java
      address isEqualTo "jon@somewhere.com"
      personalisation() containsEntriesExactlyInAnyOrder mapOf(
        "userName" to "Jon",
        "court" to DERBY_JUSTICE_CENTRE,
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
      this isInstanceOf AmendedCourtBookingPrisonCourtEmail::class.java
      address isEqualTo "jon@prison.com"
      personalisation() containsEntriesExactlyInAnyOrder mapOf(
        "court" to DERBY_JUSTICE_CENTRE,
        "courtEmailAddress" to "jon@court.com",
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
      email isEqualTo "jon@somewhere.com"
      templateName isEqualTo "user template id"
      govNotifyNotificationId isEqualTo emailNotificationId
      videoBooking isEqualTo courtBooking
      reason isEqualTo "Amended court booking"
    }
    with(notificationCaptor.secondValue) {
      email isEqualTo "jon@prison.com"
      templateName isEqualTo "prison template id"
      govNotifyNotificationId isEqualTo emailNotificationId
      videoBooking isEqualTo courtBooking
      reason isEqualTo "Amended court booking"
    }
  }

  @Test
  fun `should send emails on probation booking creation by external user`() {
    val prisoner = Prisoner(probationBookingAtBirminghamPrison.prisoner(), BIRMINGHAM, "Bob", "Builder", yesterday())

    whenever(prisonerSearchClient.getPrisoner(prisoner.prisonerNumber)) doReturn prisoner

    val request = probationBookingRequest(
      prisonCode = prisoner.prisonId!!,
      prisonerNumber = prisoner.prisonerNumber,
      appointmentDate = tomorrow(),
      startTime = LocalTime.MIDNIGHT,
      endTime = LocalTime.MIDNIGHT.plusHours(1),
    )

    whenever(createBookingService.create(request, user("facade probation team user"))) doReturn Pair(probationBookingAtBirminghamPrison, prisoner(prisonCode = prisoner.prisonId!!, prisonerNumber = prisoner.prisonerNumber, firstName = prisoner.firstName, lastName = prisoner.lastName))
    whenever(emailService.send(any<NewProbationBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")
    whenever(emailService.send(any<NewProbationBookingPrisonProbationEmail>())) doReturn Result.success(emailNotificationId to "probation template id")

    facade.create(request, user("facade probation team user"))

    inOrder(createBookingService, outboundEventsService, emailService, notificationRepository) {
      verify(createBookingService).create(request, user("facade probation team user"))
      verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CREATED, probationBookingAtBirminghamPrison.videoBookingId)
      verify(emailService).send(emailCaptor.capture())
      verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      verify(emailService).send(emailCaptor.capture())
      verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
    }

    emailCaptor.allValues hasSize 2

    with(emailCaptor.firstValue) {
      this isInstanceOf NewProbationBookingUserEmail::class.java
      address isEqualTo "jon@somewhere.com"
      personalisation() containsEntriesExactlyInAnyOrder mapOf(
        "userName" to "Jon",
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
      this isInstanceOf NewProbationBookingPrisonProbationEmail::class.java
      address isEqualTo "jon@prison.com"
      personalisation() containsEntriesExactlyInAnyOrder mapOf(
        "probationTeam" to "probation team description",
        "prison" to "Birmingham",
        "offenderNo" to "654321",
        "prisonerName" to "Bob Builder",
        "date" to tomorrow().toMediumFormatStyle(),
        "appointmentInfo" to "${birminghamLocation.localName} - 00:00 to 01:00",
        "comments" to "Probation meeting comments",
        "probationEmailAddress" to "jon@probation.com",
      )
    }

    notificationCaptor.allValues hasSize 2
    with(notificationCaptor.firstValue) {
      email isEqualTo "jon@somewhere.com"
      templateName isEqualTo "user template id"
      govNotifyNotificationId isEqualTo emailNotificationId
      videoBooking isEqualTo probationBookingAtBirminghamPrison
      reason isEqualTo "New probation booking"
    }

    with(notificationCaptor.secondValue) {
      email isEqualTo "jon@prison.com"
      templateName isEqualTo "probation template id"
      govNotifyNotificationId isEqualTo emailNotificationId
      videoBooking isEqualTo probationBookingAtBirminghamPrison
      reason isEqualTo "New probation booking"
    }
  }

  @Test
  fun `should delegate probation booking amendment to booking amendment service`() {
    val prisoner = Prisoner(probationBookingAtBirminghamPrison.prisoner(), BIRMINGHAM, "Bob", "Builder", yesterday())

    whenever(prisonerSearchClient.getPrisoner(prisoner.prisonerNumber)) doReturn prisoner

    val amendRequest = amendProbationBookingRequest(prisonCode = prisoner.prisonId!!, prisonerNumber = prisoner.prisonerNumber)

    whenever(amendBookingService.amend(1, amendRequest, user("facade probation team user"))) doReturn Pair(probationBookingAtBirminghamPrison, prisoner(prisonerNumber = prisoner.prisonerNumber, prisonCode = prisoner.prisonId!!))
    whenever(emailService.send(any<NewProbationBookingUserEmail>())) doReturn Result.success(emailNotificationId to "user template id")

    facade.amend(1, amendRequest, user("facade probation team user"))

    inOrder(amendBookingService, outboundEventsService) {
      verify(amendBookingService).amend(1, amendRequest, user("facade probation team user"))
      verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_AMENDED, 1)
    }

    // TODO test emails are sent when amend emails are ready ...
  }

  @Test
  fun `should send two court transfer emails and booking cancellation event on transfer of prisoner`() {
    val prisoner = Prisoner(
      prisonerNumber = courtBooking.prisoner(),
      prisonId = "TRN",
      firstName = "Bob",
      lastName = "Builder",
      dateOfBirth = LocalDate.EPOCH,
      lastPrisonId = MOORLAND,
    )
    whenever(contactsService.getPrimaryBookingContacts(any(), anyOrNull())) doReturn listOf(
      bookingContact(contactType = ContactType.COURT, email = "jon@court.com", name = "Jon"),
      bookingContact(contactType = ContactType.PRISON, email = "jon@prison.com", name = "Jon"),
    )

    whenever(cancelVideoBookingService.cancel(1, user("transfer user"))) doReturn courtBooking
    whenever(prisonerSearchClient.getPrisoner(courtBooking.prisoner())) doReturn prisoner
    whenever(emailService.send(any<TransferredCourtBookingCourtEmail>())) doReturn Result.success(emailNotificationId to "user template id")
    whenever(emailService.send(any<TransferredCourtBookingPrisonCourtEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

    facade.prisonerTransferred(1, user("transfer user"))

    inOrder(cancelVideoBookingService, outboundEventsService, emailService, notificationRepository) {
      verify(cancelVideoBookingService).cancel(1, user("transfer user"))
      verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, 1)
      verify(emailService).send(emailCaptor.capture())
      verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      verify(emailService).send(emailCaptor.capture())
      verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
    }

    emailCaptor.allValues hasSize 2
    with(emailCaptor.firstValue) {
      this isInstanceOf TransferredCourtBookingCourtEmail::class.java
      address isEqualTo "jon@court.com"
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
      this isInstanceOf TransferredCourtBookingPrisonCourtEmail::class.java
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

    notificationCaptor.allValues hasSize 2
    with(notificationCaptor.firstValue) {
      email isEqualTo "jon@court.com"
      templateName isEqualTo "user template id"
      govNotifyNotificationId isEqualTo emailNotificationId
      videoBooking isEqualTo courtBooking
      reason isEqualTo "Cancelled court booking due to transfer"
    }
    with(notificationCaptor.secondValue) {
      email isEqualTo "jon@prison.com"
      templateName isEqualTo "prison template id"
      govNotifyNotificationId isEqualTo emailNotificationId
      videoBooking isEqualTo courtBooking
      reason isEqualTo "Cancelled court booking due to transfer"
    }
  }

  @Test
  fun `should send one transfer email and booking cancellation event on transfer of prisoner`() {
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

    whenever(cancelVideoBookingService.cancel(1, user("transfer user"))) doReturn courtBooking
    whenever(prisonerSearchClient.getPrisoner(courtBooking.prisoner())) doReturn prisoner
    whenever(emailService.send(any<TransferredCourtBookingPrisonNoCourtEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

    facade.prisonerTransferred(1, user("transfer user"))

    inOrder(cancelVideoBookingService, outboundEventsService, emailService, notificationRepository) {
      verify(cancelVideoBookingService).cancel(1, user("transfer user"))
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
      reason isEqualTo "Cancelled court booking due to transfer"
    }
  }

  @Test
  fun `should send two release emails and booking cancellation event on release of prisoner`() {
    val prisoner = Prisoner(
      prisonerNumber = courtBooking.prisoner(),
      prisonId = "TRN",
      firstName = "Bob",
      lastName = "Builder",
      dateOfBirth = LocalDate.EPOCH,
      lastPrisonId = MOORLAND,
    )
    whenever(contactsService.getPrimaryBookingContacts(any(), anyOrNull())) doReturn listOf(
      bookingContact(contactType = ContactType.COURT, email = "jon@court.com", name = "Jon"),
      bookingContact(contactType = ContactType.PRISON, email = "jon@prison.com", name = "Jon"),
    )

    whenever(cancelVideoBookingService.cancel(1, user("release user"))) doReturn courtBooking
    whenever(prisonerSearchClient.getPrisoner(courtBooking.prisoner())) doReturn prisoner
    whenever(emailService.send(any<ReleasedCourtBookingCourtEmail>())) doReturn Result.success(emailNotificationId to "user template id")
    whenever(emailService.send(any<ReleasedCourtBookingPrisonCourtEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

    facade.prisonerReleased(1, user("release user"))

    inOrder(cancelVideoBookingService, outboundEventsService, emailService, notificationRepository) {
      verify(cancelVideoBookingService).cancel(1, user("release user"))
      verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, 1)
      verify(emailService).send(emailCaptor.capture())
      verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      verify(emailService).send(emailCaptor.capture())
      verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
    }

    emailCaptor.allValues hasSize 2
    with(emailCaptor.firstValue) {
      this isInstanceOf ReleasedCourtBookingCourtEmail::class.java
      address isEqualTo "jon@court.com"
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
      this isInstanceOf ReleasedCourtBookingPrisonCourtEmail::class.java
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

    notificationCaptor.allValues hasSize 2
    with(notificationCaptor.firstValue) {
      email isEqualTo "jon@court.com"
      templateName isEqualTo "user template id"
      govNotifyNotificationId isEqualTo emailNotificationId
      videoBooking isEqualTo courtBooking
      reason isEqualTo "Cancelled court booking due to release"
    }
    with(notificationCaptor.secondValue) {
      email isEqualTo "jon@prison.com"
      templateName isEqualTo "prison template id"
      govNotifyNotificationId isEqualTo emailNotificationId
      videoBooking isEqualTo courtBooking
      reason isEqualTo "Cancelled court booking due to release"
    }
  }

  @Test
  fun `should send one release email and booking cancellation event on release of prisoner`() {
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

    whenever(cancelVideoBookingService.cancel(1, user("release user"))) doReturn courtBooking
    whenever(prisonerSearchClient.getPrisoner(courtBooking.prisoner())) doReturn prisoner
    whenever(emailService.send(any<ReleasedCourtBookingPrisonNoCourtEmail>())) doReturn Result.success(emailNotificationId to "prison template id")

    facade.prisonerReleased(1, user("release user"))

    inOrder(cancelVideoBookingService, outboundEventsService, emailService, notificationRepository) {
      verify(cancelVideoBookingService).cancel(1, user("release user"))
      verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, 1)
      verify(emailService).send(emailCaptor.capture())
      verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
    }

    emailCaptor.allValues hasSize 1
    with(emailCaptor.firstValue) {
      this isInstanceOf ReleasedCourtBookingPrisonNoCourtEmail::class.java
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
      reason isEqualTo "Cancelled court booking due to release"
    }
  }
}

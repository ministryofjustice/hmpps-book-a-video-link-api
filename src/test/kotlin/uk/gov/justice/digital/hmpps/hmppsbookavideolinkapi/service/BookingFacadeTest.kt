package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Email
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Notification
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendCourtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendProbationBookingRequest
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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
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
  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()
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
    prisonAppointmentRepository,
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

  @BeforeEach
  fun before() {
    whenever(prisonRepository.findByCode(MOORLAND)) doReturn prison(MOORLAND)
    whenever(locationsInsidePrisonClient.getLocationsByKeys(setOf(moorlandLocation.key))) doReturn listOf(moorlandLocation)
    whenever(contactsService.getBookingContacts(any())) doReturn listOf(
      bookingContact(contactType = ContactType.OWNER, email = "jon@somewhere.com", name = "Jon"),
      bookingContact(contactType = ContactType.PRISON, email = "jon@prison.com", name = "Jon"),
    )
    whenever(prisonAppointmentRepository.findByVideoBooking(courtBooking)) doReturn courtBooking.appointments()
  }

  @Test
  fun `should send cancellation court booking emails and booking cancellation event on cancellation of court booking`() {
    val prisoner = Prisoner(courtBooking.prisoner(), MOORLAND, "Bob", "Builder")

    whenever(cancelVideoBookingService.cancel(1, "facade court user")) doReturn courtBooking
    whenever(prisonerSearchClient.getPrisoner(courtBooking.prisoner())) doReturn prisoner

    val notificationId = UUID.randomUUID()

    whenever(emailService.send(any<CancelledCourtBookingOwnerEmail>())) doReturn Result.success(notificationId to "court template id")
    whenever(emailService.send(any<CancelledCourtBookingPrisonNoCourtEmail>())) doReturn Result.success(notificationId to "prison template id")

    facade.cancel(1, "facade court user")

    inOrder(cancelVideoBookingService, outboundEventsService, emailService, notificationRepository) {
      verify(cancelVideoBookingService).cancel(1, "facade court user")
      verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CANCELLED, courtBooking.videoBookingId)
      verify(emailService).send(emailCaptor.capture())
      verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      verify(emailService).send(emailCaptor.capture())
      verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
    }

    emailCaptor.allValues hasSize 2
    with(emailCaptor.firstValue) {
      this isInstanceOf CancelledCourtBookingOwnerEmail::class.java
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
      this isInstanceOf CancelledCourtBookingPrisonNoCourtEmail::class.java
      address isEqualTo "jon@prison.com"
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
      email isEqualTo "jon@somewhere.com"
      templateName isEqualTo "court template id"
      govNotifyNotificationId isEqualTo notificationId
      videoBooking isEqualTo courtBooking
    }
    with(notificationCaptor.secondValue) {
      email isEqualTo "jon@prison.com"
      templateName isEqualTo "prison template id"
      govNotifyNotificationId isEqualTo notificationId
      videoBooking isEqualTo courtBooking
    }
  }

  @Test
  fun `should send court booking emails and booking created event on creation of court booking`() {
    val bookingRequest = courtBookingRequest(prisonCode = MOORLAND, prisonerNumber = courtBooking.prisoner())

    whenever(createBookingService.create(bookingRequest, "facade court user")) doReturn Pair(courtBooking, prisoner(prisonerNumber = courtBooking.prisoner(), prisonCode = MOORLAND))

    val notificationId = UUID.randomUUID()

    whenever(emailService.send(any<NewCourtBookingOwnerEmail>())) doReturn Result.success(notificationId to "court template id")
    whenever(emailService.send(any<NewCourtBookingPrisonNoCourtEmail>())) doReturn Result.success(notificationId to "prison template id")

    facade.create(bookingRequest, "facade court user")

    inOrder(createBookingService, outboundEventsService, emailService, notificationRepository) {
      verify(createBookingService).create(bookingRequest, "facade court user")
      verify(outboundEventsService).send(DomainEventType.VIDEO_BOOKING_CREATED, courtBooking.videoBookingId)
      verify(emailService).send(emailCaptor.capture())
      verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      verify(emailService).send(emailCaptor.capture())
      verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
    }

    emailCaptor.allValues hasSize 2
    with(emailCaptor.firstValue) {
      this isInstanceOf NewCourtBookingOwnerEmail::class.java
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
      this isInstanceOf NewCourtBookingPrisonNoCourtEmail::class.java
      address isEqualTo "jon@prison.com"
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
      email isEqualTo "jon@somewhere.com"
      templateName isEqualTo "court template id"
      govNotifyNotificationId isEqualTo notificationId
      videoBooking isEqualTo courtBooking
    }
    with(notificationCaptor.secondValue) {
      email isEqualTo "jon@prison.com"
      templateName isEqualTo "prison template id"
      govNotifyNotificationId isEqualTo notificationId
      videoBooking isEqualTo courtBooking
    }
  }

  @Test
  fun `should send court booking emails on amendment of court booking`() {
    val bookingRequest = amendCourtBookingRequest(prisonCode = MOORLAND, prisonerNumber = "123456")

    whenever(amendBookingService.amend(1, bookingRequest, "facade court user")) doReturn Pair(courtBooking, prisoner(prisonerNumber = "123456", prisonCode = MOORLAND))

    val notificationId = UUID.randomUUID()

    whenever(emailService.send(any<AmendedCourtBookingOwnerEmail>())) doReturn Result.success(notificationId to "court template id")
    whenever(emailService.send(any<AmendedCourtBookingPrisonNoCourtEmail>())) doReturn Result.success(notificationId to "prison template id")

    facade.amend(1, bookingRequest, "facade court user")

    inOrder(amendBookingService, emailService, notificationRepository) {
      verify(amendBookingService).amend(1, bookingRequest, "facade court user")
      verify(emailService).send(emailCaptor.capture())
      verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
      verify(emailService).send(emailCaptor.capture())
      verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
    }

    emailCaptor.allValues hasSize 2
    with(emailCaptor.firstValue) {
      this isInstanceOf AmendedCourtBookingOwnerEmail::class.java
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
      this isInstanceOf AmendedCourtBookingPrisonNoCourtEmail::class.java
      address isEqualTo "jon@prison.com"
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
      email isEqualTo "jon@somewhere.com"
      templateName isEqualTo "court template id"
      govNotifyNotificationId isEqualTo notificationId
      videoBooking isEqualTo courtBooking
    }
    with(notificationCaptor.secondValue) {
      email isEqualTo "jon@prison.com"
      templateName isEqualTo "prison template id"
      govNotifyNotificationId isEqualTo notificationId
      videoBooking isEqualTo courtBooking
    }
  }

  @Test
  fun `should delegate probation booking creation to booking creation service`() {
    val booking = probationBookingRequest(prisonCode = BIRMINGHAM, prisonerNumber = "123456")

    whenever(createBookingService.create(booking, "facade probation team user")) doReturn Pair(probationBooking(), prisoner(prisonerNumber = "123456", prisonCode = BIRMINGHAM))

    facade.create(booking, "facade probation team user")

    verify(createBookingService).create(booking, "facade probation team user")
  }

  @Test
  fun `should delegate probation booking amendment to booking amendment service`() {
    val booking = amendProbationBookingRequest(prisonCode = BIRMINGHAM, prisonerNumber = "123456")

    whenever(amendBookingService.amend(1, booking, "facade probation team user")) doReturn Pair(probationBooking(), prisoner(prisonerNumber = "123456", prisonCode = BIRMINGHAM))

    facade.amend(1, booking, "facade probation team user")

    verify(amendBookingService).amend(1, booking, "facade probation team user")
  }
}

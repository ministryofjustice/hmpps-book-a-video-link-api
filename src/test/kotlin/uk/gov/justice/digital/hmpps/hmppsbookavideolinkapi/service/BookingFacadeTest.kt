package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Notification
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.DERBY_JUSTICE_CENTRE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.amendCourtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.bookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsEntriesExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.moorlandLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.DomainEventType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events.OutboundEventsServiceImpl
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class BookingFacadeTest {
  private val createBookingService: CreateVideoBookingService = mock()
  private val amendBookingService: AmendVideoBookingService = mock()
  private val bookingContactsService: BookingContactsService = mock()
  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()
  private val prisonRepository: PrisonRepository = mock()
  private val emailService: EmailService = mock()
  private val notificationRepository: NotificationRepository = mock()
  private val outboundSnsEventsService: OutboundEventsServiceImpl = mock()
  private val emailCaptor = argumentCaptor<NewCourtBookingEmail>()
  private val notificationCaptor = argumentCaptor<Notification>()
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient = mock()
  private val facade = BookingFacade(
    createBookingService,
    amendBookingService,
    bookingContactsService,
    prisonAppointmentRepository,
    prisonRepository,
    emailService,
    notificationRepository,
    outboundSnsEventsService,
    locationsInsidePrisonClient,
  )

  @Test
  fun `should send court booking emails and booking created event on creation of court booking`() {
    val bookingRequest = courtBookingRequest(prisonCode = MOORLAND, prisonerNumber = "123456")
    val booking = courtBooking()
    val appointment = appointment(
      booking = booking,
      prisonCode = MOORLAND,
      prisonerNumber = "123456",
      appointmentType = "VLB_COURT_MAIN",
      date = LocalDate.of(2100, 1, 1),
      startTime = LocalTime.of(11, 0),
      endTime = LocalTime.of(11, 30),
      locationKey = moorlandLocation.key,
    )

    whenever(createBookingService.create(bookingRequest, "facade court user")) doReturn Pair(booking, prisoner(prisonerNumber = "123456", prisonCode = MOORLAND))
    whenever(prisonAppointmentRepository.findByVideoBooking(booking)) doReturn listOf(appointment)
    whenever(prisonRepository.findByCode(MOORLAND)) doReturn prison(MOORLAND)
    whenever(bookingContactsService.getBookingContacts(any())) doReturn listOf(bookingContact(contactType = ContactType.OWNER, email = "jon@somewhere.com", name = "Jon"))
    whenever(locationsInsidePrisonClient.getLocationsByKeys(setOf(moorlandLocation.key))) doReturn listOf(moorlandLocation)

    val notificationId = UUID.randomUUID()

    whenever(emailService.send(any())) doReturn Result.success(notificationId to "court template id")

    facade.create(bookingRequest, "facade court user")

    inOrder(createBookingService, outboundSnsEventsService, emailService, notificationRepository) {
      verify(createBookingService).create(bookingRequest, "facade court user")
      verify(outboundSnsEventsService).send(DomainEventType.VIDEO_BOOKING_CREATED, booking.videoBookingId)
      verify(emailService).send(emailCaptor.capture())
      verify(notificationRepository).saveAndFlush(notificationCaptor.capture())
    }

    with(emailCaptor.firstValue) {
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

    notificationCaptor.allValues hasSize 1

    with(notificationCaptor.firstValue) {
      email isEqualTo "jon@somewhere.com"
      templateName isEqualTo "court template id"
      govNotifyNotificationId isEqualTo notificationId
      videoBooking isEqualTo booking
    }
  }

  @Test
  fun `should delegate booking creation to booking creation service`() {
    val booking = probationBookingRequest(prisonCode = BIRMINGHAM, prisonerNumber = "123456")

    whenever(createBookingService.create(booking, "facade probation team user")) doReturn Pair(probationBooking(), prisoner(prisonerNumber = "123456", prisonCode = BIRMINGHAM))

    facade.create(booking, "facade probation team user")

    verify(createBookingService).create(booking, "facade probation team user")
  }

  @Test
  fun `should delegate booking amendment to booking amendment service`() {
    val booking = amendCourtBookingRequest(prisonCode = BIRMINGHAM, prisonerNumber = "123456")

    whenever(amendBookingService.amend(1, booking, "facade court user")) doReturn Pair(courtBooking(), prisoner(prisonerNumber = "123456", prisonCode = BIRMINGHAM))

    facade.amend(1, booking, "facade court user")

    verify(amendBookingService).amend(1, booking, "facade court user")
  }
}

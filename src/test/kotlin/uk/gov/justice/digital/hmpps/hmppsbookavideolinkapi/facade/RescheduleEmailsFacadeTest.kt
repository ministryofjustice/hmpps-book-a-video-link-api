package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.facade

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.EmailService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactAreaType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.RoomArea
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.bookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.locationAttributes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisonUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withPreMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withProbationPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.VideoLinkBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.NotificationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ChangeType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ContactsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.court.RescheduledCourtEmailFactory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.emails.probation.RescheduledProbationEmailFactory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class RescheduleEmailsFacadeTest {
  private val prisonRepository: PrisonRepository = mock()
  private val contactsService: ContactsService = mock()
  private val rescheduledCourtEmailFactory: RescheduledCourtEmailFactory = mock()
  private val rescheduledProbationEmailFactory: RescheduledProbationEmailFactory = mock()
  private val emailService: EmailService = mock()
  private val notificationRepository: NotificationRepository = mock()
  private val locationsService: LocationsService = mock()

  private val facade = RescheduleEmailsFacade(
    prisonRepository,
    contactsService,
    rescheduledCourtEmailFactory,
    rescheduledProbationEmailFactory,
    emailService,
    notificationRepository,
    locationsService,
  )

  private val old = bookingWithPreAndMain(
    date = tomorrow(),
    startTime = LocalTime.of(10, 0),
    endTime = LocalTime.of(11, 0),
    birminghamLocation,
  ).toModel(setOf(birminghamLocation.toModel()))

  private val amended = bookingWithMainOnly(
    date = tomorrow(),
    startTime = LocalTime.of(10, 0),
    endTime = LocalTime.of(11, 0),
    birminghamLocation,
  )

  @Nested
  @DisplayName("Consider if rescheduled")
  inner class ConsideredIfRescheduled {
    @Test
    fun `should be considered rescheduled`() {
      facade.isConsideredRescheduled(old, amended) isBool true
    }

    @Test
    fun `should not be considered rescheduled`() {
      facade.isConsideredRescheduled(amended.toModel(setOf(birminghamLocation.toModel())), amended) isBool false
    }

    @Test
    fun `should fail rescheduled check if bookings are not the same`() {
      val originalBooking: VideoLinkBooking = mock()
      whenever { originalBooking.videoLinkBookingId } doReturn 1

      val amendedBooking: VideoBooking = mock()
      whenever { amendedBooking.videoBookingId } doReturn 2

      assertThrows<IllegalArgumentException> {
        facade.isConsideredRescheduled(originalBooking, amendedBooking)
      }.message isEqualTo "Original and amended bookings must have the same video booking ID"
    }
  }

  @Nested
  @DisplayName("Booking rescheduled emails")
  inner class BookingRescheduledEmails {
    val videoBookingId = 1L

    // Mocked contacts - a mixture of prison (vcc and official visits) and probation
    val courtContacts = listOf(
      bookingContact(ContactType.PRISON, "vcc@example.com", "Prison VCC", ContactAreaType.VCC),
      bookingContact(ContactType.PRISON, "official-visits@example.com", "Prison official visits", ContactAreaType.OFFICIAL_VISITS),
      bookingContact(ContactType.COURT, "court-team@example.com", "Court team"),
    )

    val probationContacts = listOf(
      bookingContact(ContactType.PRISON, "vcc@example.com", "Prison VCC", ContactAreaType.VCC),
      bookingContact(ContactType.PRISON, "official-visits@example.com", "Prison official visits", ContactAreaType.OFFICIAL_VISITS),
      bookingContact(ContactType.PROBATION, "probation-team@example.com", "Probation team"),
    )

    val prisoner = Prisoner("A1234AA", WANDSWORTH, "John", "Briggs", tomorrow().minusYears(20))
    val user = prisonUser(name = "User Name", email = "user@example.com")

    val randomLocationUuid: UUID = UUID.randomUUID()
    val specificLocation = location(prisonCode = WANDSWORTH, id = randomLocationUuid, locationKeySuffix = "ONE", localName = "Wandsworth room 1")
    val officialVisitLocation = specificLocation.toModel(locationAttributes().copy(roomArea = RoomArea.LEGAL_VISITS))

    @BeforeEach
    internal fun setUp() {
      whenever(locationsService.getLocationById(randomLocationUuid)) doReturn officialVisitLocation
      whenever(prisonRepository.findByCode(WANDSWORTH)) doReturn prison(WANDSWORTH, true)
      whenever(contactsService.getCourtBookingContacts(any(), any(), any(), any())) doReturn courtContacts
      whenever(contactsService.getProbationBookingContacts(any(), any(), any(), any())) doReturn probationContacts
    }

    @Test()
    fun `Rescheduled emails are sent to the appropriate contacts for a court booking`() {
      val startTime = LocalTime.of(10, 0)
      val endTime = LocalTime.of(11, 0)

      val oldBooking = bookingWithPreAndMain(tomorrow(), startTime, endTime, specificLocation)
      val amendedBooking = bookingWithMainOnly(tomorrow(), startTime, endTime, specificLocation)

      facade.sendEmails(
        oldBooking = oldBooking.toModel(setOf(officialVisitLocation)),
        amendedBooking = amendedBooking,
        changeType = ChangeType.GLOBAL,
        prisoner = prisoner,
        user = user,
      )

      verify(prisonRepository).findByCode(WANDSWORTH)
      verify(locationsService).getLocationById(amendedBooking.mainHearing()!!.prisonLocationId)
      verify(contactsService).getCourtBookingContacts(
        action = BookingAction.AMEND,
        videoBookingId = amendedBooking.videoBookingId,
        locations = listOf(officialVisitLocation),
        user = user,
      )
      verify(rescheduledCourtEmailFactory).court(any(), any(), any(), any(), any())
      verify(rescheduledCourtEmailFactory, times(2)).prison(any(), any(), any(), any(), any(), any())
    }

    @Test()
    fun `Rescheduled emails are sent to the appropriate contacts for a probation booking`() {
      val startTime = LocalTime.of(10, 0)
      val endTime = LocalTime.of(11, 0)

      val oldBooking = probationBooking(tomorrow(), startTime, endTime, specificLocation)
      val amendedBooking = probationBooking(tomorrow(), startTime.plusHours(1), endTime.plusHours(1), specificLocation)

      facade.sendEmails(
        oldBooking = oldBooking.toModel(setOf(officialVisitLocation)),
        amendedBooking = amendedBooking,
        changeType = ChangeType.GLOBAL,
        prisoner = prisoner,
        user = user,
      )

      verify(prisonRepository).findByCode(WANDSWORTH)
      verify(locationsService).getLocationById(amendedBooking.probationMeeting()!!.prisonLocationId)
      verify(contactsService).getProbationBookingContacts(
        action = BookingAction.AMEND,
        videoBookingId = amendedBooking.videoBookingId,
        location = officialVisitLocation,
        user = user,
      )
      verify(rescheduledProbationEmailFactory).probation(any(), any(), any(), any(), any())
      verify(rescheduledProbationEmailFactory, times(2)).prison(any(), any(), any(), any(), any(), any())
    }
  }

  private fun bookingWithPreAndMain(date: LocalDate, startTime: LocalTime, endTime: LocalTime, location: Location) = run {
    courtBooking().withPreMainCourtPrisonAppointment(
      date = date,
      startTime = startTime,
      endTime = endTime,
      location = location,
      prisonCode = WANDSWORTH,
      prisonerNumber = "A1234AA",
    )
  }

  private fun bookingWithMainOnly(date: LocalDate, startTime: LocalTime, endTime: LocalTime, location: Location) = run {
    courtBooking().withMainCourtPrisonAppointment(
      date = date,
      startTime = startTime,
      endTime = endTime,
      location = location,
      prisonCode = WANDSWORTH,
      prisonerNumber = "A1234AA",
    )
  }

  private fun probationBooking(date: LocalDate, startTime: LocalTime, endTime: LocalTime, location: Location) = run {
    probationBooking().withProbationPrisonAppointment(
      date = date,
      startTime = startTime,
      endTime = endTime,
      location = location,
      prisonCode = WANDSWORTH,
      prisonerNumber = "A1234AA",
    )
  }
}

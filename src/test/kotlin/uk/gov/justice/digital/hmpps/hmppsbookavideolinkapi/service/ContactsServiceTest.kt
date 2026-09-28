package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactAreaType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.RoomArea
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBookingEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.facade.BookingAction
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.bookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.contact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.locationAttributes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisonUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.yesterday
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingContactsRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ContactsRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingEventRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.Long

class ContactsServiceTest {
  private val bookingContactsRepository: BookingContactsRepository = mock()
  private val contactsRepository: ContactsRepository = mock()
  private val videoBookingRepository: VideoBookingRepository = mock()
  private val videoBookingEventRepository: VideoBookingEventRepository = mock()
  private val locationsService: LocationsService = mock()

  private val service = ContactsService(
    bookingContactsRepository,
    contactsRepository,
    videoBookingRepository,
    videoBookingEventRepository,
    locationsService,
  )

  @Nested
  @DisplayName("Contacts for court booking actions")
  inner class BookingActionCourtContacts {
    val videoBookingId = 1L

    // Mocked contacts - a mixture of prison (vcc and official visits) and probation
    val bookingContacts = listOf(
      bookingContact(ContactType.PRISON, "vcc@example.com", "Prison VCC", ContactAreaType.VCC),
      bookingContact(ContactType.PRISON, "official-visits@example.com", "Prison official visits", ContactAreaType.OFFICIAL_VISITS),
      bookingContact(ContactType.COURT, "court-team@example.com", "Court team"),
    )

    val user = prisonUser(name = "User Name", email = "user@example.com")

    // Potential locations on previous booking history events
    val randomPreLocationUuid: UUID? = UUID.randomUUID()
    val randomMainLocationUuid: UUID = UUID.randomUUID()
    val randomPostLocationUuid: UUID? = UUID.randomUUID()

    // A range of different locations in different areas
    val officialVisitLocation = wandsworthLocation.toModel(locationAttributes().copy(roomArea = RoomArea.LEGAL_VISITS))
    val vccLocation = wandsworthLocation.toModel(locationAttributes().copy(roomArea = RoomArea.COURT_PROBATION))
    val undecoratedLocation = wandsworthLocation.toModel()

    @BeforeEach
    internal fun setUp() {
      whenever(bookingContactsRepository.findContactsByVideoBookingId(videoBookingId)) doReturn bookingContacts

      whenever(videoBookingEventRepository.findRecentHistoryByVideoBookingId(videoBookingId)) doReturn listOf(
        videoBookingEvent(
          eventId = 2L,
          videoBookingId = videoBookingId,
          eventType = "AMEND",
          courtBooking = true,
          preLocationId = randomPreLocationUuid,
          mainLocationId = randomMainLocationUuid,
          postLocationId = randomPostLocationUuid,
        ),
        videoBookingEvent(
          eventId = 1L,
          videoBookingId = videoBookingId,
          eventType = "CREATE",
          courtBooking = true,
          preLocationId = randomPreLocationUuid,
          mainLocationId = randomMainLocationUuid,
          postLocationId = randomPostLocationUuid,
        ),
      )
    }

    @ParameterizedTest
    @ValueSource(strings = [ "CREATE", "CANCEL", "TRANSFERRED", "RELEASED"])
    fun `Court contacts - booking ${bookingAction} - in official visits area - send to both vcc and official visit prison contacts`(bookingAction: String) {
      val contacts = service.getCourtBookingContacts(
        action = BookingAction.valueOf(bookingAction),
        videoBookingId = videoBookingId,
        locations = listOf(officialVisitLocation),
        user,
      )

      assertThat(contacts.size).isEqualTo(4)
      contacts.map { it.contactType } containsExactlyInAnyOrder listOf(ContactType.PRISON, ContactType.PRISON, ContactType.USER, ContactType.COURT)
      contacts.map { it.contactArea } containsExactlyInAnyOrder listOf(ContactAreaType.OFFICIAL_VISITS, ContactAreaType.VCC, null, null)
    }

    @ParameterizedTest
    @ValueSource(strings = [ "CREATE", "CANCEL", "TRANSFERRED", "RELEASED"])
    fun `Court contacts - booking ${bookingAction} - in vcc area - send only to vcc prison contacts`(bookingAction: String) {
      val contacts = service.getCourtBookingContacts(
        action = BookingAction.valueOf(bookingAction),
        videoBookingId = videoBookingId,
        locations = listOf(vccLocation),
        user,
      )

      assertThat(contacts.size).isEqualTo(3)
      contacts.map { it.contactType } containsExactlyInAnyOrder listOf(ContactType.PRISON, ContactType.USER, ContactType.COURT)
      contacts.map { it.contactArea } containsExactlyInAnyOrder listOf(ContactAreaType.VCC, null, null)
    }

    @ParameterizedTest
    @ValueSource(strings = [ "CREATE", "CANCEL", "TRANSFERRED", "RELEASED"])
    fun `Court contacts - action ${bookingAction} - in undecorated room with no area - send to VCC prison contacts only`(bookingAction: String) {
      val contacts = service.getCourtBookingContacts(
        action = BookingAction.valueOf(bookingAction),
        videoBookingId = videoBookingId,
        locations = listOf(undecoratedLocation),
        user,
      )

      assertThat(contacts.size).isEqualTo(3)
      contacts.map { it.contactType } containsExactlyInAnyOrder listOf(ContactType.PRISON, ContactType.USER, ContactType.COURT)
      contacts.map { it.contactArea } containsExactlyInAnyOrder listOf(ContactAreaType.VCC, null, null)
    }

    @ParameterizedTest
    @ValueSource(strings = [ "CREATE", "CANCEL", "TRANSFERRED", "RELEASED"])
    fun `Court contacts - action ${bookingAction} - in mixed VCC and official visit locations - send to all prison contacts`(bookingAction: String) {
      val contacts = service.getCourtBookingContacts(
        action = BookingAction.valueOf(bookingAction),
        videoBookingId = videoBookingId,
        locations = listOf(vccLocation, officialVisitLocation),
        user,
      )

      assertThat(contacts.size).isEqualTo(4)
      contacts.map { it.contactType } containsExactlyInAnyOrder listOf(ContactType.PRISON, ContactType.PRISON, ContactType.USER, ContactType.COURT)
      contacts.map { it.contactArea } containsExactlyInAnyOrder listOf(ContactAreaType.OFFICIAL_VISITS, ContactAreaType.VCC, null, null)
    }

    @Test
    fun `Court contacts - booking amend - in VCC area, previously in VCC area - send only to VCC contacts`() {
      // Location UUIDs returned from recent history as previous locations
      whenever(locationsService.getLocationById(randomPreLocationUuid!!)) doReturn vccLocation
      whenever(locationsService.getLocationById(randomMainLocationUuid)) doReturn vccLocation
      whenever(locationsService.getLocationById(randomPostLocationUuid!!)) doReturn vccLocation

      val contacts = service.getCourtBookingContacts(
        action = BookingAction.AMEND,
        videoBookingId = videoBookingId,
        locations = listOf(vccLocation, vccLocation, vccLocation),
        user,
      )

      assertThat(contacts.size).isEqualTo(3)
      contacts.map { it.contactType } containsExactlyInAnyOrder listOf(ContactType.PRISON, ContactType.USER, ContactType.COURT)
      contacts.map { it.contactArea } containsExactlyInAnyOrder listOf(ContactAreaType.VCC, null, null)
    }

    @Test
    fun `Court contacts - booking amend - in legal visit area, previously had a legal visit room - send to all prison contacts`() {
      // Location UUIDs returned from recent history as previous locations
      whenever(locationsService.getLocationById(randomPreLocationUuid!!)) doReturn officialVisitLocation
      whenever(locationsService.getLocationById(randomMainLocationUuid)) doReturn vccLocation
      whenever(locationsService.getLocationById(randomPostLocationUuid!!)) doReturn vccLocation

      val contacts = service.getCourtBookingContacts(
        action = BookingAction.AMEND,
        videoBookingId = videoBookingId,
        locations = listOf(officialVisitLocation, officialVisitLocation),
        user,
      )

      assertThat(contacts.size).isEqualTo(4)
      contacts.map { it.contactType } containsExactlyInAnyOrder listOf(ContactType.PRISON, ContactType.PRISON, ContactType.USER, ContactType.COURT)
      contacts.map { it.contactArea } containsExactlyInAnyOrder listOf(ContactAreaType.VCC, ContactAreaType.OFFICIAL_VISITS, null, null)
    }

    @Test
    fun `Court contacts - booking amend - in VCC area, previously had one legal visit room - send to all prison contacts`() {
      // Location UUIDs returned from recent history as previous locations
      whenever(locationsService.getLocationById(randomPreLocationUuid!!)) doReturn officialVisitLocation
      whenever(locationsService.getLocationById(randomMainLocationUuid)) doReturn vccLocation
      whenever(locationsService.getLocationById(randomPostLocationUuid!!)) doReturn vccLocation

      val contacts = service.getCourtBookingContacts(
        action = BookingAction.AMEND,
        videoBookingId = videoBookingId,
        locations = listOf(vccLocation, vccLocation),
        user,
      )

      assertThat(contacts.size).isEqualTo(4)
      contacts.map { it.contactType } containsExactlyInAnyOrder listOf(ContactType.PRISON, ContactType.PRISON, ContactType.USER, ContactType.COURT)
      contacts.map { it.contactArea } containsExactlyInAnyOrder listOf(ContactAreaType.VCC, ContactAreaType.OFFICIAL_VISITS, null, null)
    }

    @Test
    fun `Court contacts - booking amend - in undecorated room, previously had one legal visit room - send to all prison contacts`() {
      // Location UUIDs returned from recent history as previous locations
      whenever(locationsService.getLocationById(randomPreLocationUuid!!)) doReturn officialVisitLocation
      whenever(locationsService.getLocationById(randomMainLocationUuid)) doReturn undecoratedLocation
      whenever(locationsService.getLocationById(randomPostLocationUuid!!)) doReturn undecoratedLocation

      val contacts = service.getCourtBookingContacts(
        action = BookingAction.AMEND,
        videoBookingId = videoBookingId,
        locations = listOf(undecoratedLocation, undecoratedLocation),
        user,
      )

      assertThat(contacts.size).isEqualTo(4)
      contacts.map { it.contactType } containsExactlyInAnyOrder listOf(ContactType.PRISON, ContactType.PRISON, ContactType.USER, ContactType.COURT)
      contacts.map { it.contactArea } containsExactlyInAnyOrder listOf(ContactAreaType.VCC, ContactAreaType.OFFICIAL_VISITS, null, null)
    }

    @Test
    fun `Court contacts - booking amend - in undecorated room, previously had one VCC room - send only to VCC prison contacts`() {
      // Location UUIDs returned from recent history as previous locations
      whenever(locationsService.getLocationById(randomPreLocationUuid!!)) doReturn vccLocation
      whenever(locationsService.getLocationById(randomMainLocationUuid)) doReturn undecoratedLocation
      whenever(locationsService.getLocationById(randomPostLocationUuid!!)) doReturn undecoratedLocation

      val contacts = service.getCourtBookingContacts(
        action = BookingAction.AMEND,
        videoBookingId = videoBookingId,
        locations = listOf(undecoratedLocation, undecoratedLocation, undecoratedLocation),
        user,
      )

      assertThat(contacts.size).isEqualTo(3)
      contacts.map { it.contactType } containsExactlyInAnyOrder listOf(ContactType.PRISON, ContactType.USER, ContactType.COURT)
      contacts.map { it.contactArea } containsExactlyInAnyOrder listOf(ContactAreaType.VCC, null, null)
    }

    @Test
    fun `Court contacts - other booking action COURT_HEARING_LINK_REMINDER - send only to VCC prison contacts`() {
      // No locations looked up as no history check on non-AMEND booking actions regardless of rooms

      val contacts = service.getCourtBookingContacts(
        action = BookingAction.COURT_HEARING_LINK_REMINDER,
        videoBookingId = videoBookingId,
        locations = listOf(officialVisitLocation, vccLocation, undecoratedLocation),
        user,
      )

      assertThat(contacts.size).isEqualTo(3)
      contacts.map { it.contactType } containsExactlyInAnyOrder listOf(ContactType.PRISON, ContactType.USER, ContactType.COURT)
      contacts.map { it.contactArea } containsExactlyInAnyOrder listOf(ContactAreaType.VCC, null, null)

      verifyNoInteractions(locationsService)
    }
  }

  @Nested
  @DisplayName("Contacts for probation booking actions")
  inner class BookingActionProbationContacts {
    val videoBookingId = 1L

    // Mocked contacts - a mixture of prison (vcc and official visits) and probation
    val bookingContacts = listOf(
      bookingContact(ContactType.PRISON, "vcc@example.com", "Prison VCC", ContactAreaType.VCC),
      bookingContact(ContactType.PRISON, "official-visits@example.com", "Prison official visits", ContactAreaType.OFFICIAL_VISITS),
      bookingContact(ContactType.PROBATION, "probation-team@example.com", "Probation team"),
    )

    val user = prisonUser(name = "User Name", email = "user@example.com")

    // A range of different locations in different areas
    val officialVisitLocation = wandsworthLocation.toModel(locationAttributes().copy(roomArea = RoomArea.LEGAL_VISITS))
    val vccLocation = wandsworthLocation.toModel(locationAttributes().copy(roomArea = RoomArea.COURT_PROBATION))
    val undecoratedLocation = wandsworthLocation.toModel()

    // Potential locations on previous booking history events
    val randomLocationUuid: UUID = UUID.randomUUID()

    @BeforeEach
    internal fun setUp() {
      whenever(bookingContactsRepository.findContactsByVideoBookingId(videoBookingId)) doReturn bookingContacts

      whenever(videoBookingEventRepository.findRecentHistoryByVideoBookingId(videoBookingId)) doReturn listOf(
        videoBookingEvent(
          eventId = 2L,
          videoBookingId = videoBookingId,
          eventType = "AMEND",
          courtBooking = false,
          mainLocationId = randomLocationUuid,
        ),
        videoBookingEvent(
          eventId = 1L,
          videoBookingId = videoBookingId,
          eventType = "CREATE",
          courtBooking = false,
          mainLocationId = randomLocationUuid,
        ),
      )
    }

    @ParameterizedTest
    @ValueSource(strings = [ "CREATE", "CANCEL", "TRANSFERRED", "RELEASED"])
    fun `Probation contacts - action ${bookingAction} - in official visits area - send only to official visit prison contacts`(bookingAction: String) {
      val contacts = service.getProbationBookingContacts(BookingAction.valueOf(bookingAction), videoBookingId, officialVisitLocation, user)

      assertThat(contacts.size).isEqualTo(3)
      contacts.map { it.contactType } containsExactlyInAnyOrder listOf(ContactType.PRISON, ContactType.USER, ContactType.PROBATION)
      contacts.map { it.contactArea } containsExactlyInAnyOrder listOf(ContactAreaType.OFFICIAL_VISITS, null, null)
    }

    @ParameterizedTest
    @ValueSource(strings = [ "CREATE", "CANCEL", "TRANSFERRED", "RELEASED"])
    fun `Probation contacts - action ${bookingAction} - in VCC area - send to VCC prison contacts only`(bookingAction: String) {
      val contacts = service.getProbationBookingContacts(BookingAction.valueOf(bookingAction), videoBookingId, vccLocation, user)

      assertThat(contacts.size).isEqualTo(3)
      contacts.map { it.contactType } containsExactlyInAnyOrder listOf(ContactType.PRISON, ContactType.USER, ContactType.PROBATION)
      contacts.map { it.contactArea } containsExactlyInAnyOrder listOf(ContactAreaType.VCC, null, null)
    }

    @ParameterizedTest
    @ValueSource(strings = [ "CREATE", "CANCEL", "TRANSFERRED", "RELEASED"])
    fun `Probation contacts - action ${bookingAction} - in undecorated room with no area - send to VCC prison contacts only`(bookingAction: String) {
      val contacts = service.getProbationBookingContacts(BookingAction.valueOf(bookingAction), videoBookingId, undecoratedLocation, user)

      contacts.map { it.contactType } containsExactlyInAnyOrder listOf(ContactType.PRISON, ContactType.USER, ContactType.PROBATION)
      contacts.map { it.contactArea } containsExactlyInAnyOrder listOf(ContactAreaType.VCC, null, null)
    }

    @Test
    fun `Probation contacts - booking amend - in official visit area, previously official visit room - send to official visit prison contact only`() {
      // Location UUID returned from recent history as previous location
      whenever(locationsService.getLocationById(randomLocationUuid)) doReturn officialVisitLocation

      val contacts = service.getProbationBookingContacts(BookingAction.AMEND, videoBookingId, officialVisitLocation, user)

      assertThat(contacts.size).isEqualTo(3)
      contacts.map { it.contactType } containsExactlyInAnyOrder listOf(ContactType.PRISON, ContactType.USER, ContactType.PROBATION)
      contacts.map { it.contactArea } containsExactlyInAnyOrder listOf(ContactAreaType.OFFICIAL_VISITS, null, null)
    }

    @Test
    fun `Probation contacts - booking amend - in official visit area, previously undecorated room - send to official visit prison contacts only`() {
      // Location UUID returned from recent history as previous location
      whenever(locationsService.getLocationById(randomLocationUuid)) doReturn undecoratedLocation

      val contacts = service.getProbationBookingContacts(BookingAction.AMEND, videoBookingId, officialVisitLocation, user)

      assertThat(contacts.size).isEqualTo(3)
      contacts.map { it.contactType } containsExactlyInAnyOrder listOf(ContactType.PRISON, ContactType.USER, ContactType.PROBATION)
      contacts.map { it.contactArea } containsExactlyInAnyOrder listOf(ContactAreaType.OFFICIAL_VISITS, null, null)
    }

    @Test
    fun `Probation contacts - booking amend - in undecorated room, previously in official visit room - send to all prison contacts`() {
      // Location UUID returned from recent history as previous location
      whenever(locationsService.getLocationById(randomLocationUuid)) doReturn officialVisitLocation

      val contacts = service.getProbationBookingContacts(BookingAction.AMEND, videoBookingId, undecoratedLocation, user)

      assertThat(contacts.size).isEqualTo(4)
      contacts.map { it.contactType } containsExactlyInAnyOrder listOf(ContactType.PRISON, ContactType.PRISON, ContactType.USER, ContactType.PROBATION)
      contacts.map { it.contactArea } containsExactlyInAnyOrder listOf(ContactAreaType.OFFICIAL_VISITS, ContactAreaType.VCC, null, null)
    }

    @Test
    fun `Probation contacts - booking amend - in VCC room, previously in VCC room - send to VCC prison contacts only`() {
      // Location UUID returned from recent history as previous location
      whenever(locationsService.getLocationById(randomLocationUuid)) doReturn vccLocation

      val contacts = service.getProbationBookingContacts(BookingAction.AMEND, videoBookingId, vccLocation, user)

      assertThat(contacts.size).isEqualTo(3)
      contacts.map { it.contactType } containsExactlyInAnyOrder listOf(ContactType.PRISON, ContactType.USER, ContactType.PROBATION)
      contacts.map { it.contactArea } containsExactlyInAnyOrder listOf(ContactAreaType.VCC, null, null)
    }

    @Test
    fun `Probation contacts - booking amend - in VCC room, previously in undecorated room - send to VCC prison contacts only`() {
      // Location UUID returned from recent history as previous location
      whenever(locationsService.getLocationById(randomLocationUuid)) doReturn undecoratedLocation

      val contacts = service.getProbationBookingContacts(BookingAction.AMEND, videoBookingId, vccLocation, user)

      assertThat(contacts.size).isEqualTo(3)
      contacts.map { it.contactType } containsExactlyInAnyOrder listOf(ContactType.PRISON, ContactType.USER, ContactType.PROBATION)
      contacts.map { it.contactArea } containsExactlyInAnyOrder listOf(ContactAreaType.VCC, null, null)
    }

    @Test
    fun `Probation contacts - booking amend - in undecorated room, previously in VCC room - send to VCC prison contacts only`() {
      // Location UUID returned from recent history as previous location
      whenever(locationsService.getLocationById(randomLocationUuid)) doReturn vccLocation

      val contacts = service.getProbationBookingContacts(BookingAction.AMEND, videoBookingId, undecoratedLocation, user)

      assertThat(contacts.size).isEqualTo(3)
      contacts.map { it.contactType } containsExactlyInAnyOrder listOf(ContactType.PRISON, ContactType.USER, ContactType.PROBATION)
      contacts.map { it.contactArea } containsExactlyInAnyOrder listOf(ContactAreaType.VCC, null, null)
    }

    @Test
    fun `Probation contacts - booking amend - in undecorated room, previously undecorated - send to VCC prison contacts only`() {
      // Location UUID returned from recent history as previous location
      whenever(locationsService.getLocationById(randomLocationUuid)) doReturn undecoratedLocation

      val contacts = service.getProbationBookingContacts(BookingAction.AMEND, videoBookingId, undecoratedLocation, user)

      assertThat(contacts.size).isEqualTo(3)
      contacts.map { it.contactType } containsExactlyInAnyOrder listOf(ContactType.PRISON, ContactType.USER, ContactType.PROBATION)
      contacts.map { it.contactArea } containsExactlyInAnyOrder listOf(ContactAreaType.VCC, null, null)
    }

    @Test
    fun `Probation contacts - booking amend - in VCC room, previously official visit area - send to ALL prison contacts`() {
      // Location UUID returned from recent history as previous location
      whenever(locationsService.getLocationById(randomLocationUuid)) doReturn officialVisitLocation

      val contacts = service.getProbationBookingContacts(BookingAction.AMEND, videoBookingId, vccLocation, user)

      assertThat(contacts.size).isEqualTo(4)
      contacts.map { it.contactType } containsExactlyInAnyOrder listOf(ContactType.PRISON, ContactType.PRISON, ContactType.USER, ContactType.PROBATION)
      contacts.map { it.contactArea } containsExactlyInAnyOrder listOf(ContactAreaType.VCC, ContactAreaType.OFFICIAL_VISITS, null, null)
    }

    @Test
    fun `Probation contacts - booking amend - in official visit room, previously in VCC room - send to ALL prison contacts`() {
      // Location UUID returned from recent history as previous location
      whenever(locationsService.getLocationById(randomLocationUuid)) doReturn vccLocation

      val contacts = service.getProbationBookingContacts(BookingAction.AMEND, videoBookingId, officialVisitLocation, user)

      assertThat(contacts.size).isEqualTo(4)
      contacts.map { it.contactType } containsExactlyInAnyOrder listOf(ContactType.PRISON, ContactType.PRISON, ContactType.USER, ContactType.PROBATION)
      contacts.map { it.contactArea } containsExactlyInAnyOrder listOf(ContactAreaType.VCC, ContactAreaType.OFFICIAL_VISITS, null, null)
    }

    @Test
    fun `Probation contacts - other booking action - COURT_HEARING_LINK_REMINDER - send to VCC prison contacts only`() {
      // This will not happen in real life as the court hearing link reminder is only sent to court contacts
      val contacts = service.getProbationBookingContacts(BookingAction.COURT_HEARING_LINK_REMINDER, videoBookingId, undecoratedLocation, user)

      assertThat(contacts.size).isEqualTo(3)
      contacts.map { it.contactType } containsExactlyInAnyOrder listOf(ContactType.PRISON, ContactType.USER, ContactType.PROBATION)
      contacts.map { it.contactArea } containsExactlyInAnyOrder listOf(ContactAreaType.VCC, null, null)

      verifyNoInteractions(locationsService)
    }
  }

  @Nested
  @DisplayName("Contacts for requested bookings")
  inner class BookingRequestContacts {
    @Test
    fun `getContactsForCourtBookingRequest should return contacts`() {
      val court = court()
      val prison = prison()

      val courtContact = contact(ContactType.COURT, "court.contact@example.com", "Court contact")
      val prisonContact = contact(ContactType.PRISON, "prison.contact@example.com", "Prison contact")

      whenever(
        contactsRepository.findContactsByContactTypeAndCodeAndPrimaryContactTrue(
          ContactType.COURT,
          court.code,
        ),
      ) doReturn listOf(courtContact)
      whenever(
        contactsRepository.findContactsByContactTypeAndCodeAndPrimaryContactTrue(
          ContactType.PRISON,
          prison.code,
        ),
      ) doReturn listOf(prisonContact)

      val result = service.getContactsForCourtBookingRequest(court, prison, courtUser(name = "User Name"))

      result hasSize 3
      result.containsAll(listOf(courtContact, prisonContact)) isBool true
      result.any { it.name == "User Name" && it.primaryContact } isBool true
    }

    @Test
    fun `getContactsForProbationBookingRequest should return contacts including contact area type for prison contacts`() {
      val probationTeam = probationTeam()
      val prison = prison()

      val probationContact = contact(ContactType.PROBATION, "probation.contact@example.com", "Probation contact")
      val prisonContact =
        contact(ContactType.PRISON, "prison.contact@example.com", "Prison contact", ContactAreaType.VCC)

      whenever(
        contactsRepository.findContactsByContactTypeAndCodeAndPrimaryContactTrue(
          ContactType.PROBATION,
          probationTeam.code,
        ),
      ) doReturn listOf(probationContact)
      whenever(
        contactsRepository.findContactsByContactTypeAndCodeAndPrimaryContactTrue(
          ContactType.PRISON,
          prison.code,
        ),
      ) doReturn listOf(prisonContact)

      val result =
        service.getContactsForProbationBookingRequest(probationTeam, prison, probationUser(name = "User Name"))

      result hasSize 3
      result.containsAll(listOf(probationContact, prisonContact)) isBool true
      result.any { it.name == "User Name" && it.primaryContact } isBool true
      result.any { it.contactArea == ContactAreaType.VCC } isBool true
    }

    @Test
    fun `getContactsForProbationBookingRequest should NOT return prison contacts for the OFFICIAL_VISITS area`() {
      val probationTeam = probationTeam()
      val prison = prison()

      val probationContact = contact(ContactType.PROBATION, "probation.contact@example.com", "Probation contact")
      val prisonContact =
        contact(ContactType.PRISON, "prison.contact@example.com", "Prison contact", ContactAreaType.OFFICIAL_VISITS)

      whenever(
        contactsRepository.findContactsByContactTypeAndCodeAndPrimaryContactTrue(
          ContactType.PROBATION,
          probationTeam.code,
        ),
      ) doReturn listOf(probationContact)
      whenever(
        contactsRepository.findContactsByContactTypeAndCodeAndPrimaryContactTrue(
          ContactType.PRISON,
          prison.code,
        ),
      ) doReturn listOf(prisonContact)

      val result =
        service.getContactsForProbationBookingRequest(probationTeam, prison, probationUser(name = "User Name"))

      result hasSize 2
      result.containsAll(listOf(probationContact)) isBool true
      result.any { it.name == "User Name" && it.primaryContact } isBool true
      result.any { it.contactArea == ContactAreaType.OFFICIAL_VISITS } isBool false
    }
  }

  fun videoBookingEvent(
    eventId: Long,
    videoBookingId: Long,
    eventType: String, // CREATE, AMEND, CANCEL
    courtBooking: Boolean,
    dateOfBooking: LocalDate = yesterday(),
    prisonCode: String = WANDSWORTH,
    mainLocationId: UUID = UUID.randomUUID(),
    preLocationId: UUID? = null,
    postLocationId: UUID? = null,
  ) = VideoBookingEvent(
    eventId = eventId,
    videoBookingId = videoBookingId,
    dateOfBooking = dateOfBooking,
    timestamp = yesterday().atTime(8, 0),
    timestampDatePart = yesterday(),
    eventType = eventType,
    prisonCode = prisonCode,
    courtDescription = if (courtBooking) "Court" else null,
    courtCode = if (courtBooking) "CCDECC" else null,
    probationTeamDescription = if (!courtBooking) "Barnet" else null,
    probationTeamCode = if (!courtBooking) "BARNET" else null,
    createdByPrison = false,
    preLocationId = preLocationId?.let { preLocationId },
    preDate = preLocationId?.let { tomorrow() },
    preStartTime = preLocationId?.let { LocalTime.of(9, 45) },
    preEndTime = preLocationId?.let { LocalTime.of(10, 0) },
    mainLocationId = mainLocationId,
    mainDate = tomorrow(),
    mainStartTime = LocalTime.of(10, 0),
    mainEndTime = LocalTime.of(11, 0),
    postLocationId = postLocationId?.let { postLocationId },
    postDate = postLocationId?.let { tomorrow() },
    postStartTime = postLocationId.let { LocalTime.of(11, 0) },
    postEndTime = postLocationId?.let { LocalTime.of(11, 15) },
    courtBooking = courtBooking,
    type = if (courtBooking) "Remand hearing" else "Other",
    user = "G484XX",
  )
}

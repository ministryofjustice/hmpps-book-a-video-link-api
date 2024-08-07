package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.bookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.contact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.user
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingContactsRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ContactsRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import java.util.Optional

class ContactsServiceTest {
  private val bookingContactsRepository: BookingContactsRepository = mock()
  private val contactsRepository: ContactsRepository = mock()
  private val videoBookingRepository: VideoBookingRepository = mock()
  private val userService: UserService = mock()

  private val service = ContactsService(bookingContactsRepository, contactsRepository, videoBookingRepository, userService)

  @Test
  fun `getAllBookingContacts should return contacts`() {
    val videoBookingId = 1L
    val bookingContact = bookingContact(ContactType.PRISON, "prison.contact@example.com", "Prison Contact")
    val booking = courtBooking("createdByUser").apply { amendedBy = "amendedByUser" }

    whenever(bookingContactsRepository.findContactsByVideoBookingId(videoBookingId)) doReturn listOf(bookingContact)
    whenever(videoBookingRepository.findById(videoBookingId)) doReturn Optional.of(booking)
    whenever(userService.getUser("createdByUser")) doReturn user(name = "Created User", email = "created@example.com")
    whenever(userService.getUser("amendedByUser")) doReturn user(name = "Amended User", email = "amended@example.com")

    val result = service.getAllBookingContacts(videoBookingId)

    result hasSize 3
    result.any { it.name == "Created User" && it.primaryContact } isBool true
    result.any { it.name == "Amended User" && it.primaryContact } isBool true
  }

  @Test
  fun `getAllBookingContacts should throw EntityNotFoundException when no booking found`() {
    val videoBookingId = 1L
    whenever(videoBookingRepository.findById(videoBookingId)) doReturn Optional.empty()

    val exception = assertThrows<EntityNotFoundException> { service.getAllBookingContacts(videoBookingId) }

    exception.message isEqualTo "Video booking with ID $videoBookingId not found"
  }

  @Test
  fun `getContactsForCourtBookingRequest should return contacts`() {
    val court = court()
    val prison = prison()

    val courtContact = contact(ContactType.COURT, "court.contact@example.com", "Court contact")
    val prisonContact = contact(ContactType.PRISON, "prison.contact@example.com", "Prison contact")

    whenever(contactsRepository.findContactsByContactTypeAndCodeAndPrimaryContactTrue(ContactType.COURT, court.code)) doReturn listOf(courtContact)
    whenever(contactsRepository.findContactsByContactTypeAndCodeAndPrimaryContactTrue(ContactType.PRISON, prison.code)) doReturn listOf(prisonContact)

    val result = service.getContactsForCourtBookingRequest(court, prison, user(name = "User Name"))

    result hasSize 3
    result.containsAll(listOf(courtContact, prisonContact)) isBool true
    result.any { it.name == "User Name" && it.primaryContact } isBool true
  }

  @Test
  fun `getContactsForProbationBookingRequest should return contacts`() {
    val probationTeam = probationTeam()
    val prison = prison()

    val probationContact = contact(ContactType.PROBATION, "probation.contact@example.com", "Probation contact")
    val prisonContact = contact(ContactType.PRISON, "prison.contact@example.com", "Prison contact")

    whenever(contactsRepository.findContactsByContactTypeAndCodeAndPrimaryContactTrue(ContactType.PROBATION, probationTeam.code)) doReturn listOf(probationContact)
    whenever(contactsRepository.findContactsByContactTypeAndCodeAndPrimaryContactTrue(ContactType.PRISON, prison.code)) doReturn listOf(prisonContact)

    val result = service.getContactsForProbationBookingRequest(probationTeam, prison, user(name = "User Name"))

    result hasSize 3
    result.containsAll(listOf(probationContact, prisonContact)) isBool true
    result.any { it.name == "User Name" && it.primaryContact } isBool true
  }

  @Test
  fun `getPrimaryBookingContacts should return user contact only when emails same`() {
    whenever(videoBookingRepository.findById(1L)) doReturn Optional.of(courtBooking())

    val bookingContact = bookingContact(ContactType.PRISON, "prison.contact@example.com", "Prison Contact")

    whenever(bookingContactsRepository.findContactsByVideoBookingIdAndPrimaryContactTrue(1L)) doReturn listOf(bookingContact)

    val result = service.getPrimaryBookingContacts(1L, user(name = "User Name", email = "prison.contact@example.com"))

    result.single().contactType isEqualTo ContactType.USER
  }

  @Test
  fun `getPrimaryBookingContacts should return user contact and prison contact when emails differ`() {
    whenever(videoBookingRepository.findById(1L)) doReturn Optional.of(courtBooking())

    val bookingContact = bookingContact(ContactType.PRISON, "prison.contact@example.com", "Prison Contact")

    whenever(bookingContactsRepository.findContactsByVideoBookingIdAndPrimaryContactTrue(1L)) doReturn listOf(bookingContact)

    val result = service.getPrimaryBookingContacts(1L, user(name = "User Name", email = "prisoner.contact@example.com"))

    result.map { it.contactType } containsExactlyInAnyOrder listOf(ContactType.PRISON, ContactType.USER)
  }

  @Test
  fun `getPrimaryBookingContacts should return prison contact only when user is service user`() {
    whenever(videoBookingRepository.findById(1L)) doReturn Optional.of(courtBooking())

    val bookingContact = bookingContact(ContactType.PRISON, "prison.contact@example.com", "Prison Contact")

    whenever(bookingContactsRepository.findContactsByVideoBookingIdAndPrimaryContactTrue(1L)) doReturn listOf(bookingContact)

    val result = service.getPrimaryBookingContacts(1L, user(name = "User Name", userType = UserType.SERVICE, email = "blah"))

    result.single().contactType isEqualTo ContactType.PRISON
  }

  @Test
  fun `getPrimaryBookingContacts should return prison contact only when user has no email`() {
    whenever(videoBookingRepository.findById(1L)) doReturn Optional.of(courtBooking())

    val bookingContact = bookingContact(ContactType.PRISON, "prison.contact@example.com", "Prison Contact")

    whenever(bookingContactsRepository.findContactsByVideoBookingIdAndPrimaryContactTrue(1L)) doReturn listOf(bookingContact)

    val result = service.getPrimaryBookingContacts(1L, user(name = "User Name", email = null))

    result.single().contactType isEqualTo ContactType.PRISON
  }
}

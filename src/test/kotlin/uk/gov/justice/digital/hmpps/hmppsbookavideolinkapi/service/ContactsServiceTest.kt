package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.bookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.contact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prison
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
  fun `getBookingContacts should return contacts`() {
    val videoBookingId = 1L
    val bookingContact = bookingContact(ContactType.PRISON, "prison.contact@example.com", "Prison Contact")
    val booking = courtBooking("createdByUser").apply { amendedBy = "amendedByUser" }

    whenever(bookingContactsRepository.findContactsByVideoBookingId(videoBookingId)) doReturn listOf(bookingContact)
    whenever(videoBookingRepository.findById(videoBookingId)) doReturn Optional.of(booking)
    whenever(userService.getContactDetails("createdByUser")) doReturn ContactDetails("Created User", "created@example.com")
    whenever(userService.getContactDetails("amendedByUser")) doReturn ContactDetails("Amended User", "amended@example.com")

    val result = service.getBookingContacts(videoBookingId)

    result hasSize 3
    result.any { it.name == "Created User" && it.primaryContact } isBool true
    result.any { it.name == "Amended User" && it.primaryContact } isBool true
  }

  @Test
  fun `getBookingContacts should throw EntityNotFoundException when no booking found`() {
    val videoBookingId = 1L
    whenever(videoBookingRepository.findById(videoBookingId)) doReturn Optional.empty()

    val exception = assertThrows<EntityNotFoundException> { service.getBookingContacts(videoBookingId) }

    exception.message isEqualTo "Video booking with ID $videoBookingId not found"
  }

  @Test
  fun `getContactsForCourtBookingRequest should return contacts`() {
    val court = court()
    val prison = prison()
    val username = "user"

    val courtContact = contact(ContactType.COURT, "court.contact@example.com", "Court contact")
    val prisonContact = contact(ContactType.PRISON, "prison.contact@example.com", "Prison contact")
    val userContactDetails = ContactDetails("User Name", "user@example.com")

    whenever(contactsRepository.findContactsByContactTypeAndCode(ContactType.COURT, court.code)) doReturn listOf(courtContact)
    whenever(contactsRepository.findContactsByContactTypeAndCode(ContactType.PRISON, prison.code)) doReturn listOf(prisonContact)
    whenever(userService.getContactDetails(username)) doReturn userContactDetails

    val result = service.getContactsForCourtBookingRequest(court, prison, username)

    result hasSize 3
    result.containsAll(listOf(courtContact, prisonContact)) isBool true
    result.any { it.name == "User Name" && it.primaryContact } isBool true
  }

  @Test
  fun `getContactsForCourtBookingRequest should return only court and prison contacts if user not found`() {
    val court = court()
    val prison = prison()
    val username = "user"

    val courtContact = contact(ContactType.COURT, "court.contact@example.com", "Court contact")
    val prisonContact = contact(ContactType.PRISON, "prison.contact@example.com", "Prison contact")

    whenever(contactsRepository.findContactsByContactTypeAndCode(ContactType.COURT, court.code)) doReturn listOf(courtContact)
    whenever(contactsRepository.findContactsByContactTypeAndCode(ContactType.PRISON, prison.code)) doReturn listOf(prisonContact)
    whenever(userService.getContactDetails(username)) doReturn null

    val result = service.getContactsForCourtBookingRequest(court, prison, username)

    result hasSize 2
    result.containsAll(listOf(courtContact, prisonContact)) isBool true
  }
}

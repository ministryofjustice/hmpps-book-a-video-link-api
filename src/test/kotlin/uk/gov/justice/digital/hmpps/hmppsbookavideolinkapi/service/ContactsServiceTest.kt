package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.ManageUsersClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.userContactDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.userDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.userEmail
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
  fun `should get created by contact using username`() {
    val booking = courtBooking(createdBy = "test_user")

    whenever(videoBookingRepository.findById(booking.videoBookingId)) doReturn Optional.of(booking)
    whenever(bookingContactsRepository.findContactsByVideoBookingId(booking.videoBookingId)) doReturn emptyList()
    whenever(userService.getContactDetails("test_user")) doReturn userContactDetails("Test User Name", "test_user@email.com")

    val contact = service.getBookingContacts(booking.videoBookingId)

    with(contact.single()) {
      name isEqualTo "Test User Name"
      email isEqualTo "test_user@email.com"
    }
  }

  @Test
  fun `should get created by contact using email`() {
    val booking = courtBooking(createdBy = "test_user@email.com")

    whenever(videoBookingRepository.findById(booking.videoBookingId)) doReturn Optional.of(booking)
    whenever(bookingContactsRepository.findContactsByVideoBookingId(booking.videoBookingId)) doReturn emptyList()
    whenever(userService.getContactDetails("test_user")) doReturn userContactDetails("Test User Name", "test_user@email.com")

    val contact = service.getBookingContacts(booking.videoBookingId)

    with(contact.single()) {
      name isEqualTo "Test User Name"
      email isEqualTo "test_user@email.com"
    }
  }

  @Test
  fun `should get created by and amended by contacts using username`() {
    val booking = courtBooking(createdBy = "create_user").apply { amendedBy = "amend_user" }

    whenever(videoBookingRepository.findById(booking.videoBookingId)) doReturn Optional.of(booking)
    whenever(bookingContactsRepository.findContactsByVideoBookingId(booking.videoBookingId)) doReturn emptyList()
    whenever(userService.getContactDetails("create_user")) doReturn userContactDetails("Create User Name", "create_user@email.com")
    whenever(userService.getContactDetails("amend_user")) doReturn userContactDetails("Amend User Name", "amend_user@email.com")

    val contacts = service.getBookingContacts(booking.videoBookingId)

    contacts hasSize 2

    with(contacts[0]) {
      name isEqualTo "Create User Name"
      email isEqualTo "create_user@email.com"
    }

    with(contacts[1]) {
      name isEqualTo "Amend User Name"
      email isEqualTo "amend_user@email.com"
    }
  }

  @Test
  fun `should get created by and amended by contacts using email`() {
    val booking = courtBooking(createdBy = "create_user@email.com").apply { amendedBy = "amend_user@email.com" }

    whenever(videoBookingRepository.findById(booking.videoBookingId)) doReturn Optional.of(booking)
    whenever(bookingContactsRepository.findContactsByVideoBookingId(booking.videoBookingId)) doReturn emptyList()
    whenever(userService.getContactDetails("create_user")) doReturn userContactDetails("Create User Name", "create_user@email.com")
    whenever(userService.getContactDetails("amend_user")) doReturn userContactDetails("Amend User Name", "amend_user@email.com")

    val contacts = service.getBookingContacts(booking.videoBookingId)

    contacts hasSize 2

    with(contacts[0]) {
      name isEqualTo "Create User Name"
      email isEqualTo "create_user@email.com"
    }

    with(contacts[1]) {
      name isEqualTo "Amend User Name"
      email isEqualTo "amend_user@email.com"
    }
  }
}

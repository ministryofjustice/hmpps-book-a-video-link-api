package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.mockito.Mockito.mock
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingContactsRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ContactsRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository

class ContactsServiceTest {
  private val bookingContactsRepository: BookingContactsRepository = mock()
  private val contactsRepository: ContactsRepository = mock()
  private val videoBookingRepository: VideoBookingRepository = mock()
  private val userService: UserService = mock()

  private val service = ContactsService(bookingContactsRepository, contactsRepository, videoBookingRepository, userService)
}

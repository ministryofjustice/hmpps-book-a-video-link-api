package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Contact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.BookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingContactsRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ContactsRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel

@Service
class ContactsService(
  private val bookingContactsRepository: BookingContactsRepository,
  private val contactsRepository: ContactsRepository,
  private val videoBookingRepository: VideoBookingRepository,
  private val userService: UserService,
) {

  fun getBookingContacts(videoBookingId: Long): List<BookingContact> {
    // Get the contact details of people set up as contacts for the prison, court or probation team
    val listOfContacts = bookingContactsRepository.findContactsByVideoBookingId(videoBookingId).toModel().toMutableList()

    // Get the booking itself, to find the createdBy and amendedBy usernames
    val booking = videoBookingRepository.findById(videoBookingId)
      .orElseThrow { EntityNotFoundException("Video booking with ID $videoBookingId not found") }

    val mayBeCreatedByContactDetails = userService.getContactDetails(booking.createdBy)

    if (mayBeCreatedByContactDetails?.email != null) {
      listOfContacts.add(
        BookingContact(
          videoBookingId = videoBookingId,
          contactType = ContactType.OWNER,
          name = mayBeCreatedByContactDetails.name,
          email = mayBeCreatedByContactDetails.email,
          primaryContact = true,
        ),
      )
    }

    // Include the person who amended this booking as a second owner, if different
    if (booking.amendedBy != null && booking.amendedBy != booking.createdBy) {
      val mayBeAmendedByContactDetails = userService.getContactDetails(booking.amendedBy!!)

      if (mayBeAmendedByContactDetails?.email != null) {
        listOfContacts.add(
          BookingContact(
            videoBookingId = videoBookingId,
            contactType = ContactType.OWNER,
            name = mayBeAmendedByContactDetails.name,
            email = mayBeAmendedByContactDetails.email,
            primaryContact = true,
          ),
        )
      }
    }

    return listOfContacts.toList()
  }

  fun getContactsForCourtBookingRequest(court: Court, prison: Prison, username: String): List<Contact> {
    val courtContacts = contactsRepository.findContactsByContactTypeAndCode(ContactType.COURT, court.code)
    val prisonContacts = contactsRepository.findContactsByContactTypeAndCode(ContactType.PRISON, prison.code)
    val userContact = userService.getContactDetails(username)?.let {
      Contact(
        contactType = ContactType.OWNER,
        code = "USER",
        name = it.name,
        email = it.email,
        primaryContact = true,
      )
    }

    return courtContacts + prisonContacts + listOfNotNull(userContact)
  }
}

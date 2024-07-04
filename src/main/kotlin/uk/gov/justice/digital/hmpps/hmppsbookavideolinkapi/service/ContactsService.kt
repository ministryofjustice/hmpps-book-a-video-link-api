package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Contact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ProbationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingContactsRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ContactsRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository

@Service
class ContactsService(
  private val bookingContactsRepository: BookingContactsRepository,
  private val contactsRepository: ContactsRepository,
  private val videoBookingRepository: VideoBookingRepository,
  private val userService: UserService,
) {

  fun getBookingContacts(videoBookingId: Long): List<BookingContact> {
    // Get the contact details of people set up as contacts for the prison, court or probation team
    val listOfContacts = bookingContactsRepository.findContactsByVideoBookingId(videoBookingId).toMutableList()

    // Get the booking itself, to find the createdBy and amendedBy usernames
    val booking = videoBookingRepository.findById(videoBookingId)
      .orElseThrow { EntityNotFoundException("Video booking with ID $videoBookingId not found") }

    userService.getContactDetails(booking.createdBy)?.also {
      listOfContacts.add(
        BookingContact(
          videoBookingId = videoBookingId,
          contactType = ContactType.OWNER,
          name = it.name,
          email = it.email,
          primaryContact = true,
        ),
      )
    }

    // Include the person who amended this booking as a second owner, if different
    if (booking.amendedBy != null && booking.amendedBy != booking.createdBy) {
      userService.getContactDetails(booking.amendedBy!!)?.also {
        listOfContacts.add(
          BookingContact(
            videoBookingId = videoBookingId,
            contactType = ContactType.OWNER,
            name = it.name,
            email = it.email,
            primaryContact = true,
          ),
        )
      }
    }

    return listOfContacts.toList()
  }

  fun getContactsForCourtBookingRequest(court: Court, prison: Prison, username: String) = buildContactsListForBookingRequest(
    contactType = ContactType.COURT,
    agencyCode = court.code,
    prisonCode = prison.code,
    username = username,
  )

  fun getContactsForProbationBookingRequest(probationTeam: ProbationTeam, prison: Prison, username: String) = buildContactsListForBookingRequest(
    contactType = ContactType.PROBATION,
    agencyCode = probationTeam.code,
    prisonCode = prison.code,
    username = username,
  )

  private fun buildContactsListForBookingRequest(
    contactType: ContactType,
    agencyCode: String,
    prisonCode: String,
    username: String,
  ): List<Contact> {
    val primaryContacts = contactsRepository.findContactsByContactTypeAndCode(contactType, agencyCode)
    val prisonContacts = contactsRepository.findContactsByContactTypeAndCode(ContactType.PRISON, prisonCode)

    val userContact = userService.getContactDetails(username)?.let {
      Contact(
        contactType = ContactType.OWNER,
        code = "USER",
        name = it.name,
        email = it.email,
        primaryContact = true,
      )
    }
    return primaryContacts + prisonContacts + listOfNotNull(userContact)
  }
}

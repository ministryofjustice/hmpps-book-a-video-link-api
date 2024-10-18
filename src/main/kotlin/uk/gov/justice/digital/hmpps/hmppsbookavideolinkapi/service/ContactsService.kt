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
  fun getAllBookingContacts(videoBookingId: Long): List<BookingContact> {
    // Get the booking itself, to find the createdBy and amendedBy usernames
    val booking = videoBookingRepository.findById(videoBookingId)
      .orElseThrow { EntityNotFoundException("Video booking with ID $videoBookingId not found") }

    return buildList {
      // Get the contact details of people set up as contacts for the prison, court or probation team
      addAll(bookingContactsRepository.findContactsByVideoBookingId(videoBookingId))

      userService.getUser(booking.createdBy)?.let {
        add(
          BookingContact(
            videoBookingId = videoBookingId,
            contactType = ContactType.USER,
            name = it.name,
            email = it.mayBeEmail(),
            primaryContact = true,
          ),
        )
      }

      // Include the person who amended this booking as a second user, if different
      if (booking.amendedBy != null && booking.amendedBy != booking.createdBy) {
        userService.getUser(booking.amendedBy!!)?.let {
          add(
            BookingContact(
              videoBookingId = videoBookingId,
              contactType = ContactType.USER,
              name = it.name,
              email = it.mayBeEmail(),
              primaryContact = true,
            ),
          )
        }
      }
    }
  }

  fun getBookingContacts(videoBookingId: Long, user: User): List<BookingContact> {
    videoBookingRepository.findById(videoBookingId).orElseThrow { EntityNotFoundException("Video booking with ID $videoBookingId not found") }

    val userContact = user.mayBeEmail()?.let { email ->
      BookingContact(
        videoBookingId = videoBookingId,
        contactType = ContactType.USER,
        name = user.name,
        email = email,
        primaryContact = true,
      )
    }

    return bookingContactsRepository.findContactsByVideoBookingIdAndPrimaryContactTrue(videoBookingId).filter { it.email != userContact?.email } + listOfNotNull(userContact)
  }

  fun getContactsForCourtBookingRequest(court: Court, prison: Prison, user: User) = buildContactsListForBookingRequest(
    contactType = ContactType.COURT,
    agencyCode = court.code,
    prisonCode = prison.code,
    user = user,
  )

  fun getContactsForProbationBookingRequest(probationTeam: ProbationTeam, prison: Prison, user: User) = buildContactsListForBookingRequest(
    contactType = ContactType.PROBATION,
    agencyCode = probationTeam.code,
    prisonCode = prison.code,
    user = user,
  )

  private fun buildContactsListForBookingRequest(
    contactType: ContactType,
    agencyCode: String,
    prisonCode: String,
    user: User,
  ): List<Contact> {
    val contacts = contactsRepository.findContactsByContactTypeAndCodeAndPrimaryContactTrue(contactType, agencyCode)
    val prisonContacts = contactsRepository.findContactsByContactTypeAndCodeAndPrimaryContactTrue(ContactType.PRISON, prisonCode)

    val userContact = user.let {
      Contact(
        contactType = ContactType.USER,
        code = "USER",
        name = it.name,
        email = it.mayBeEmail(),
        primaryContact = true,
      )
    }
    return contacts.filter { it.email != userContact.email } + prisonContacts + listOfNotNull(userContact)
  }

  private fun User.mayBeEmail() = when (this) {
    is ExternalUser -> email
    is PrisonUser -> email
    else -> null
  }
}

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Contact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactAreaType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ProbationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.RoomArea
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.facade.BookingAction
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
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
  /**
   * This function is only used by the contact controller endpoint to return contacts
   * related to a booking. It is not currently called in PRODUCTION by any consumers.
   * Candidate to remove.
   */
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

  // =============== new functions below ==================

  /**
   * Implements email routing for COURT bookings based on location
   * attributes, contact types, and booking actions.
   */
  fun getCourtBookingContacts(
    action: BookingAction,
    videoBookingId: Long,
    locations: List<Location>,
    user: User,
  ): List<BookingContact> {
    // Get the user's email as a booking contact, if they have an email address present
    val userContact = getUserContactIfHasAnEmailAddress(videoBookingId, user)

    // Get all contacts (court, prison) related to this booking
    val unfilteredContacts = bookingContactsRepository.findContactsByVideoBookingId(videoBookingId)
      .filter { it.email != userContact?.email }
      .filter { it.contactType != ContactType.PROBATION } + listOfNotNull(userContact)

    val filteredContacts = when (action) {
      BookingAction.CREATE -> when {
        locations.containsOnlyVccLocations() -> unfilteredContacts.filter { it.contactArea == ContactAreaType.VCC || it.contactType == ContactType.COURT }
        locations.containsOnlyLegalVisitLocations() -> unfilteredContacts.filter { it.contactArea == ContactAreaType.OFFICIAL_VISITS || it.contactType == ContactType.COURT }
        else -> unfilteredContacts
      }

      BookingAction.AMEND -> when {
        // Will be extra logic here to use VideoBookingEvents - to check whether this booking was previously in a different area
        locations.containsOnlyVccLocations() -> unfilteredContacts.filter { it.contactArea == ContactAreaType.VCC || it.contactType == ContactType.COURT }
        locations.containsOnlyLegalVisitLocations() -> unfilteredContacts.filter { it.contactArea == ContactAreaType.OFFICIAL_VISITS || it.contactType == ContactType.COURT }
        else -> unfilteredContacts
      }

      BookingAction.CANCEL, BookingAction.TRANSFERRED, BookingAction.RELEASED -> when {
        locations.containsOnlyVccLocations() -> unfilteredContacts.filter { it.contactArea == ContactAreaType.VCC || it.contactType == ContactType.COURT }
        locations.containsOnlyLegalVisitLocations() -> unfilteredContacts.filter { it.contactArea == ContactAreaType.OFFICIAL_VISITS || it.contactType == ContactType.COURT }
        else -> unfilteredContacts
      }

      else -> {
        // Same as previously before email routing
        unfilteredContacts
      }
    }

    return filteredContacts
  }

  /**
   * Implements email routing for PROBATION bookings based on location
   * attributes, contact types, and booking actions.
   */
  fun getProbationBookingContacts(
    action: BookingAction,
    videoBookingId: Long,
    location: Location,
    user: User,
  ): List<BookingContact> {
    // Get the user's email as a booking contact if they have an email address present
    val userContact = getUserContactIfHasAnEmailAddress(videoBookingId, user)

    // Get all contacts (probation, prison) related to this booking
    val unfilteredContacts = bookingContactsRepository.findContactsByVideoBookingId(videoBookingId)
      .filter { it.email != userContact?.email }
      .filter { it.contactType != ContactType.COURT } + listOfNotNull(userContact)

    val filteredContacts = when (action) {
      BookingAction.CREATE -> when {
        location.isAVccLocation() -> unfilteredContacts.filter { it.contactArea == ContactAreaType.VCC || it.contactType == ContactType.PROBATION }
        location.isALegalVisitLocation() -> unfilteredContacts.filter { it.contactArea == ContactAreaType.OFFICIAL_VISITS || it.contactType == ContactType.PROBATION }
        else -> unfilteredContacts
      }

      BookingAction.AMEND -> when {
        // Will be extra logic here to use VideoBookingEvents - to check whether this booking was previously in a different area
        location.isAVccLocation() -> unfilteredContacts.filter { it.contactArea == ContactAreaType.VCC || it.contactType == ContactType.PROBATION }
        location.isALegalVisitLocation() -> unfilteredContacts.filter { it.contactArea == ContactAreaType.OFFICIAL_VISITS || it.contactType == ContactType.PROBATION }
        else -> unfilteredContacts
      }

      BookingAction.CANCEL, BookingAction.TRANSFERRED, BookingAction.RELEASED -> when {
        location.isAVccLocation() -> unfilteredContacts.filter { it.contactArea == ContactAreaType.VCC || it.contactType == ContactType.PROBATION }
        location.isALegalVisitLocation() -> unfilteredContacts.filter { it.contactArea == ContactAreaType.OFFICIAL_VISITS || it.contactType == ContactType.PROBATION }
        else -> unfilteredContacts
      }

      else -> {
        // Same as previously before email routing
        unfilteredContacts
      }
    }

    return filteredContacts
  }

  private fun getUserContactIfHasAnEmailAddress(videoBookingId: Long, user: User) = user.mayBeEmail()?.let { email ->
    BookingContact(
      videoBookingId = videoBookingId,
      contactType = ContactType.USER,
      name = user.name,
      email = email,
      primaryContact = true,
    )
  }

  fun Location.isAVccLocation(): Boolean = this.extraAttributes?.roomArea == RoomArea.COURT_PROBATION
  fun Location.isALegalVisitLocation(): Boolean = this.extraAttributes?.roomArea == RoomArea.LEGAL_VISITS

  fun List<Location>.containsOnlyVccLocations(): Boolean = all { it.extraAttributes?.roomArea == RoomArea.COURT_PROBATION }

  fun List<Location>.containsOnlyLegalVisitLocations(): Boolean = all { it.extraAttributes?.roomArea == RoomArea.LEGAL_VISITS }

  // =============== new functions above ==================

  /**
   * This function is CURRENTLY used by the email facade to retrieve contacts for a particular booking.
   * It will be superseded by the specific court and probation functions above.
   */
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

    return bookingContactsRepository.findContactsByVideoBookingId(videoBookingId).filter { it.email != userContact?.email } + listOfNotNull(userContact)
  }

  // ----- Below this line deals with Request Bookings - which are always routed to the COURT_PROBATION prison contacts -----

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
    is DeliusUser -> email
    else -> null
  }
}

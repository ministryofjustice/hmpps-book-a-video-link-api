package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingEventRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService

@Service
class ContactsService(
  private val bookingContactsRepository: BookingContactsRepository,
  private val contactsRepository: ContactsRepository,
  private val videoBookingRepository: VideoBookingRepository,
  private val videoBookingEventRepository: VideoBookingEventRepository,
  private val locationsService: LocationsService,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * Contacts for Court bookings.
   * This function builds a list of contacts for the notifications for a booking when changes are made.
   *
   * It implements the "email routing" rules by identifying superset of contacts (user, prison, court) and
   * filtering this list using rules based on the type of booking, type of action, type of prison location, and
   * the type of contact.
   */
  fun getCourtBookingContacts(action: BookingAction, videoBookingId: Long, locations: List<Location>, user: User): List<BookingContact> {
    // Construct a set of all contacts that could be relevant to this booking
    // This will naturally avoid PROBATION contacts for COURT bookings, and vice versa.
    val allContacts = buildSet {
      getUserContactIfHasAnEmailAddress(videoBookingId, user)?.let { add(it) }
      addAll(bookingContactsRepository.findContactsByVideoBookingId(videoBookingId))
    }

    val filteredContacts = when (action) {
      BookingAction.CREATE, BookingAction.CANCEL, BookingAction.TRANSFERRED, BookingAction.RELEASED -> {
        if (locations.containsNoLegalVisitLocations()) {
          allContacts.vccContacts()
        } else {
          allContacts
        }
      }

      BookingAction.AMEND -> {
        val previousLocations: List<Location> = getLocationsForPreviousCourtBookingHistory(videoBookingId, user)
        if (locations.containsNoLegalVisitLocations() && previousLocations.containsNoLegalVisitLocations()) {
          // Omit legal visit contacts if no legal visit rooms are on the current or previous version of the booking
          allContacts.vccContacts()
        } else {
          allContacts
        }
      }

      else -> allContacts.vccContacts()
    }

    return filteredContacts.toList()
  }

  /**
   * Contacts for Probation bookings.
   * This function builds a list of contacts for the notifications for a booking when changes are made.
   *
   * It implements the "email routing" rules by identifying superset of contacts (user, prison, probation team) and
   * filtering this list using rules based on the type of booking, type of action, type of prison location, and
   * the type of contact.
   */
  fun getProbationBookingContacts(action: BookingAction, videoBookingId: Long, location: Location, user: User): List<BookingContact> {
    // Construct a set of all contacts that could be relevant to this booking
    // This will naturally avoid COURT contacts on PROBATION bookings, and vice versa.
    val allContacts = buildSet {
      getUserContactIfHasAnEmailAddress(videoBookingId, user)?.let { add(it) }
      addAll(bookingContactsRepository.findContactsByVideoBookingId(videoBookingId).filter { it.contactType != ContactType.COURT })
    }

    val filteredContacts = when (action) {
      BookingAction.CREATE, BookingAction.CANCEL, BookingAction.TRANSFERRED, BookingAction.RELEASED -> {
        if (location.isALegalVisitLocation()) {
          allContacts.legalVisitContacts()
        } else {
          allContacts.vccContacts()
        }
      }

      BookingAction.AMEND -> {
        val previousLocation: Location? = getLocationForPreviousProbationBookingHistory(videoBookingId, user)

        when {
          // Current location is legal, previous was legal or undefined
          location.isALegalVisitLocation() && (previousLocation?.isALegalVisitLocation() == true || previousLocation?.isInAnUndefinedArea() == true) -> {
            allContacts.legalVisitContacts()
          }

          // Current location is VCC, previous was VCC or undefined
          location.isAVccLocation() && (previousLocation?.isAVccLocation() == true || previousLocation?.isInAnUndefinedArea() == true) -> {
            allContacts.vccContacts()
          }

          // Current location is undefined, previous was VCC or undefined
          location.isInAnUndefinedArea() && (previousLocation?.isAVccLocation() == true || previousLocation?.isInAnUndefinedArea() == true) -> {
            allContacts.vccContacts()
          }

          // All other combinations are sent to all booking contacts
          else -> allContacts
        }
      }

      else -> allContacts.vccContacts()
    }

    return filteredContacts.toList()
  }

  /**
   * Function to examine the user object for an email address and to construct a BookingContact for them.
   */
  private fun getUserContactIfHasAnEmailAddress(videoBookingId: Long, user: User) = user.mayBeEmail()?.let { email ->
    BookingContact(
      videoBookingId = videoBookingId,
      contactType = ContactType.USER,
      name = user.name,
      email = email,
      primaryContact = true,
    )
  }

  /**
   * Extension function for a set of booking contacts to filter the superset to a narrower list
   * of prison contacts, retaining user, court and probation contacts where they exist.
   */
  private fun Set<BookingContact>.vccContacts(): List<BookingContact> = this.filter {
    (it.contactType == ContactType.PRISON && it.contactArea == ContactAreaType.VCC) ||
      it.contactType == ContactType.COURT ||
      it.contactType == ContactType.PROBATION ||
      it.contactType == ContactType.USER
  }

  /**
   * Extension function for a set of booking contacts to filter the superset to a narrower list
   * of prison contacts, retaining user, court and probation contacts where they exist.
   */
  private fun Set<BookingContact>.legalVisitContacts(): List<BookingContact> = this.filter {
    (it.contactType == ContactType.PRISON && it.contactArea == ContactAreaType.OFFICIAL_VISITS) ||
      it.contactType == ContactType.COURT ||
      it.contactType == ContactType.PROBATION ||
      it.contactType == ContactType.USER
  }

  /**
   * Private utility functions for checking location types (from room decoration)
   */
  private fun Location.isInAnUndefinedArea(): Boolean = this.extraAttributes?.roomArea == null
  private fun Location.isAVccLocation(): Boolean = this.extraAttributes?.roomArea == RoomArea.COURT_PROBATION
  private fun Location.isALegalVisitLocation(): Boolean = this.extraAttributes?.roomArea == RoomArea.LEGAL_VISITS
  private fun List<Location>.containsOnlyVccLocations(): Boolean = all { it.extraAttributes?.roomArea == RoomArea.COURT_PROBATION }
  private fun List<Location>.containsOnlyLegalVisitLocations(): Boolean = all { it.extraAttributes?.roomArea == RoomArea.LEGAL_VISITS }
  private fun List<Location>.containsOnlyUndefinedLocations(): Boolean = all { it.extraAttributes?.roomArea == null }
  private fun List<Location>.containsNoLegalVisitLocations(): Boolean = none { it.extraAttributes?.roomArea == RoomArea.LEGAL_VISITS }

  /**
   * Function to get the last two video booking events (history rows) to compare the previous location to the current
   * location for a probation booking, which influences which prison contacts should receive notifications.
   */
  private fun getLocationForPreviousProbationBookingHistory(videoBookingId: Long, user: User): Location? {
    log.info("GetLocationForPreviousProbationBookingHistory for $videoBookingId")

    // Get the recent events for this videoBookingId
    val historyEvents = videoBookingEventRepository.findRecentHistoryByVideoBookingId(videoBookingId)
    if (historyEvents.isEmpty() || historyEvents.size < 2) {
      log.info("GetLocationForPreviousProbationBookingHistory - History events did not return at least two recent events, size was ${historyEvents.size}")
      return null
    }

    // Should be 2 rows - First one an UPDATE event (just done) and the second one is the CREATE/UPDATE event we are interested in
    val eventOfInterest = historyEvents[1]
    val location = locationsService.getLocationById(eventOfInterest.mainLocationId)
    log.info("GetLocationForPreviousProbationBookingHistory - Returning probation location for ${location?.dpsLocationId} ${location?.description} IsVCC ${location?.isAVccLocation()} IsLegal ${location?.isALegalVisitLocation()}")
    return location
  }

  /**
   * Function to get the last two video booking events (history rows) to compare the previous locations to the current
   * locations for court bookings, which influences which prison contacts should receive notifications.
   */
  private fun getLocationsForPreviousCourtBookingHistory(videoBookingId: Long, user: User): List<Location> {
    log.info("GetLocationsForPreviousCourtBookingHistory for $videoBookingId")

    // Get the recent events for this videoBookingId
    val historyEvents = videoBookingEventRepository.findRecentHistoryByVideoBookingId(videoBookingId)
    if (historyEvents.isEmpty() || historyEvents.size < 2) {
      log.info("GetLocationsForPreviousCourtBookingHistory - History events did not return at least two recent events, size was ${historyEvents.size}")
      return emptyList()
    }

    // Should be 2 rows - First one an UPDATE event (just done) and the second one is either a CREATE or UPDATE event we are interested in
    val eventOfInterest = historyEvents[1]

    // Extract all the locations from the history row - should be main + optional pre and post
    val locationsOnEvent = listOfNotNull(eventOfInterest.mainLocationId, eventOfInterest.preLocationId, eventOfInterest.postLocationId)

    // For each location get the data + room decoration (if present)
    val locations = locationsOnEvent.mapNotNull { loc ->
      locationsService.getLocationById(loc)
    }

    locations.forEach { location ->
      log.info("location ${location.dpsLocationId} ${location.description} IsVCC ${location.isAVccLocation()} IsLegal ${location.isALegalVisitLocation()} IsUndecorated ${location.isInAnUndefinedArea()}")
    }

    return locations
  }

  /**
   * Rescheduled email contacts
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

  /**
   * Requested bookings (court) - have no final locations yet - and are therefore directed only to the VCC contacts in prisons
   * Never to the LEGAL VISIT prison contacts.
   */
  fun getContactsForCourtBookingRequest(court: Court, prison: Prison, user: User) = buildContactsListForBookingRequest(
    contactType = ContactType.COURT,
    agencyCode = court.code,
    prisonCode = prison.code,
    user = user,
  )

  /**
   * Requested bookings (probation) - have no final locations yet - and are therefore directed only to the VCC contacts in prisons
   * Never to the LEGAL VISIT prison contacts.
   */
  fun getContactsForProbationBookingRequest(probationTeam: ProbationTeam, prison: Prison, user: User) = buildContactsListForBookingRequest(
    contactType = ContactType.PROBATION,
    agencyCode = probationTeam.code,
    prisonCode = prison.code,
    user = user,
  )

  /**
   * Private function used to find contacts for booking requests - with no final locations on them.
   */
  private fun buildContactsListForBookingRequest(
    contactType: ContactType,
    agencyCode: String,
    prisonCode: String,
    user: User,
  ): List<Contact> {
    val contacts = contactsRepository.findContactsByContactTypeAndCodeAndPrimaryContactTrue(contactType, agencyCode)

    // For booking requests we want to avoid sending notifications to the OFFICIAL_VISIT prison contacts
    val prisonContacts = contactsRepository.findContactsByContactTypeAndCodeAndPrimaryContactTrue(ContactType.PRISON, prisonCode)
      .filter { it.contactArea != ContactAreaType.OFFICIAL_VISITS }

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

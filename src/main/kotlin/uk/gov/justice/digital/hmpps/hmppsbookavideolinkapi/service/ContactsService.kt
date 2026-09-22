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
  private val userService: UserService,
  private val videoBookingEventRepository: VideoBookingEventRepository,
  private val locationsService: LocationsService,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

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

  fun getCourtBookingContacts(action: BookingAction, videoBookingId: Long, locations: List<Location>, user: User): List<BookingContact> {
    // Construct a set of all contacts that could be relevant to this booking
    // This will naturally avoid PROBATION contacts for COURT bookings, and vice versa.
    val allContacts = buildSet {
      getUserContactIfHasAnEmailAddress(videoBookingId, user)?.let { add(it) }
      addAll(bookingContactsRepository.findContactsByVideoBookingId(videoBookingId))
    }

    val filteredContacts = when (action) {
      BookingAction.CREATE, BookingAction.CANCEL, BookingAction.TRANSFERRED, BookingAction.RELEASED -> {
        if (locations.containsOnlyVccLocations()) {
          allContacts.vccContacts()
        } else {
          allContacts
        }
      }

      BookingAction.AMEND -> {
        val previousLocations: List<Location> = getLocationsForPreviousCourtBookingHistory(videoBookingId, user)

        when {
          // Current is in VCC rooms only and previous has only VCC or undefined rooms
          locations.containsOnlyVccLocations() && (previousLocations.containsOnlyVccLocations() || previousLocations.containsOnlyUndefinedLocations()) -> {
            allContacts.vccContacts()
          }

          // Current is in the legal visits are and previous only has legal or undefined rooms
          locations.containsOnlyLegalVisitLocations() && (previousLocations.containsOnlyLegalVisitLocations() || previousLocations.containsOnlyUndefinedLocations()) -> {
            allContacts.legalVisitContacts()
          }

          // Current is in an undefined area and previous also in an undefined area
          locations.containsOnlyUndefinedLocations() && previousLocations.containsOnlyUndefinedLocations() -> {
            allContacts.vccContacts()
          }

          // Default is to send to all VCC and LEGAL prison contacts
          else -> allContacts
        }
      }

      else -> allContacts.vccContacts()
    }

    return filteredContacts.toList()
  }

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
          // Current location is legal, previous location legal or undefined
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

  private fun getUserContactIfHasAnEmailAddress(videoBookingId: Long, user: User) = user.mayBeEmail()?.let { email ->
    BookingContact(
      videoBookingId = videoBookingId,
      contactType = ContactType.USER,
      name = user.name,
      email = email,
      primaryContact = true,
    )
  }

  fun Set<BookingContact>.vccContacts(): List<BookingContact> = this.filter {
    (it.contactType == ContactType.PRISON && it.contactArea == ContactAreaType.VCC) ||
      it.contactType == ContactType.COURT ||
      it.contactType == ContactType.PROBATION ||
      it.contactType == ContactType.USER
  }

  fun Set<BookingContact>.legalVisitContacts(): List<BookingContact> = this.filter {
    (it.contactType == ContactType.PRISON && it.contactArea == ContactAreaType.OFFICIAL_VISITS) ||
      it.contactType == ContactType.COURT ||
      it.contactType == ContactType.PROBATION ||
      it.contactType == ContactType.USER
  }

  fun Location.isInAnUndefinedArea(): Boolean = this.extraAttributes?.roomArea == null
  fun Location.isAVccLocation(): Boolean = this.extraAttributes?.roomArea == RoomArea.COURT_PROBATION
  fun Location.isALegalVisitLocation(): Boolean = this.extraAttributes?.roomArea == RoomArea.LEGAL_VISITS
  fun List<Location>.containsOnlyVccLocations(): Boolean = all { it.extraAttributes?.roomArea == RoomArea.COURT_PROBATION }
  fun List<Location>.containsOnlyLegalVisitLocations(): Boolean = all { it.extraAttributes?.roomArea == RoomArea.LEGAL_VISITS }
  fun List<Location>.containsOnlyUndefinedLocations(): Boolean = all { it.extraAttributes?.roomArea == null }
  fun List<Location>.containsAnyLegalVisitLocations(): Boolean = any { it.extraAttributes?.roomArea == RoomArea.LEGAL_VISITS }
  fun List<Location>.containsAnyVccLocations(): Boolean = any { it.extraAttributes?.roomArea == RoomArea.COURT_PROBATION }
  fun List<Location>.containsAnyUndefinedLocations(): Boolean = any { it.extraAttributes?.roomArea == null }

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

    // Extract all the locations - should be main + optional pre and post
    val locationsOnEvent = listOfNotNull(eventOfInterest.mainLocationId, eventOfInterest.preLocationId, eventOfInterest.postLocationId)

    val locations = locationsOnEvent.mapNotNull { loc ->
      locationsService.getLocationById(loc)
    }

    locations.forEach { location ->
      log.info("location ${location.dpsLocationId} ${location.description} IsVCC ${location.isAVccLocation()} IsLegal ${location.isALegalVisitLocation()} IsUndecorated ${location.isInAnUndefinedArea()}")
    }

    return locations
  }

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

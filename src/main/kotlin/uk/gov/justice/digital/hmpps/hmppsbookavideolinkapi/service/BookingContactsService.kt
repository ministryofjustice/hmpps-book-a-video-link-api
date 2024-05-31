package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.manageusers.ManageUsersClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.isEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.BookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingContactsRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel

@Service
class BookingContactsService(
  private val bookingContactsRepository: BookingContactsRepository,
  private val videoBookingRepository: VideoBookingRepository,
  private val manageUsersClient: ManageUsersClient,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getBookingContacts(videoBookingId: Long): List<BookingContact> {
    // Get the contact details of people set up as contacts for the prison, court or probation team
    val listOfContacts = bookingContactsRepository.findContactsForBooking(videoBookingId).toModel().toMutableList()

    // Get the booking itself, to find the createdBy and amendedBy usernames
    val booking = videoBookingRepository.findById(videoBookingId)
      .orElseThrow { EntityNotFoundException("Video booking with ID $videoBookingId not found") }

    val mayBeCreatedByContactDetails = getContactDetails(booking.createdBy)

    if (mayBeCreatedByContactDetails?.email != null) {
      listOfContacts.add(
        BookingContact(
          videoBookingId = videoBookingId,
          contactType = ContactType.OWNER,
          name = mayBeCreatedByContactDetails.name,
          email = mayBeCreatedByContactDetails.email,
        ),
      )
    }

    // Include the person who amended this booking as a second owner, if different
    if (booking.amendedBy != null && booking.amendedBy != booking.createdBy) {
      val mayBeAmendedByContactDetails = getContactDetails(booking.amendedBy!!)

      if (mayBeAmendedByContactDetails?.email != null) {
        listOfContacts.add(
          BookingContact(
            videoBookingId = videoBookingId,
            contactType = ContactType.OWNER,
            name = mayBeAmendedByContactDetails.name,
            email = mayBeAmendedByContactDetails.email,
          ),
        )
      }
    }

    return listOfContacts.toList()
  }

  private fun getContactDetails(username: String): ContactDetails? {
    val mayBeUserDetails = manageUsersClient.getUsersDetails(username) ?: return null

    return if (username.isEmail()) {
      ContactDetails(name = mayBeUserDetails.name, email = username)
    } else {
      return ContactDetails(name = mayBeUserDetails.name, email = manageUsersClient.getUsersEmail(username)?.email)
    }
  }
  private data class ContactDetails(val name: String, val email: String?)
}

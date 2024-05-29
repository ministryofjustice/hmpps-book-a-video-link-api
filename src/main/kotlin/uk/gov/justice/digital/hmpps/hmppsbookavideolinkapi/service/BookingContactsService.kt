package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.isEmail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.BookingContact
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.ContactType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingContactsRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel

@Service
class BookingContactsService(
  val bookingContactsRepository: BookingContactsRepository,
  val videoBookingRepository: VideoBookingRepository,
) {
  fun getBookingContacts(videoBookingId: Long): List<BookingContact> {
    // Get the contact details of people set up as contacts for the prison, court or probation team
    val listOfContacts = bookingContactsRepository.findContactsForBooking(videoBookingId).toModel().toMutableList()

    // Get the booking itself, to find the createdBy and amendedBy usernames
    val booking = videoBookingRepository.findById(videoBookingId)
      .orElseThrow { EntityNotFoundException("Video booking with ID $videoBookingId not found") }

    // TODO: Call manage-user-api to get full name and email (GET /users/{username}, GET /users/{username}/email)

    // For now - check if the username is an email address and return it as the owner contact
    if (booking.createdBy.isEmail()) {
      listOfContacts.add(
        BookingContact(
          videoBookingId = videoBookingId,
          contactType = ContactType.OWNER,
          name = booking.createdBy,
          email = booking.createdBy,
        ),
      )
    }

    // Include the person who amended this booking as a second owner, if different
    if (booking.amendedBy != null && booking.amendedBy != booking.createdBy) {
      if (booking.amendedBy!!.isEmail()) {
        listOfContacts.add(
          BookingContact(
            videoBookingId = videoBookingId,
            contactType = ContactType.OWNER,
            name = booking.amendedBy,
            email = booking.amendedBy,
          ),
        )
      }
    }

    return listOfContacts
  }
}

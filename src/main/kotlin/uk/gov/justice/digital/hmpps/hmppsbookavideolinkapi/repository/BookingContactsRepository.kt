package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingContact

/**
 * This repository is read-only and accessed via the view v_booking_contacts.
 */
@Repository
interface BookingContactsRepository : ReadOnlyRepository<BookingContact, Long> {
  fun findContactsByVideoBookingId(videoBookingId: Long): List<BookingContact>
  fun findContactsByVideoBookingIdAndPrimaryContactTrue(videoBookingId: Long): List<BookingContact>
}

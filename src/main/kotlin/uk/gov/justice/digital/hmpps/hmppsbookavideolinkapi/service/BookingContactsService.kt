package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingContactsRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel

@Service
class BookingContactsService(val bookingContactsRepository: BookingContactsRepository) {
  fun getBookingContacts(
    videoBookingId: Long,
  ) = bookingContactsRepository.findContactsForBooking(videoBookingId).toModel()
}

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.BookingHistoryRepository

@Service
class BookingHistoryService(private val bookingHistoryRepository: BookingHistoryRepository) {

  fun getHistoryByVideoBookingId(videoBookingId: Long): List<BookingHistory> =
    bookingHistoryRepository.findAllByVideoBookingId(videoBookingId)

  fun getHistoryByBookingHistoryId(bookingHistoryId: Long): BookingHistory =
    bookingHistoryRepository.findById(bookingHistoryId)
      .orElseThrow(EntityNotFoundException())

}

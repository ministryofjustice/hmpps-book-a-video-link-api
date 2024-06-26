package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistory

@Repository
interface BookingHistoryRepository : JpaRepository<BookingHistory, Long> {
  fun findAllByVideoBookingIdOrderByCreatedTime(videoBookingId: Long): List<BookingHistory>
}

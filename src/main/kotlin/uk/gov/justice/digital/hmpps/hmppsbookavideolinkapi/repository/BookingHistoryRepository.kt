package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistory
import java.time.LocalDateTime

@Repository
interface BookingHistoryRepository : JpaRepository<BookingHistory, Long> {
  fun findAllByVideoBookingIdOrderByCreatedTime(videoBookingId: Long): List<BookingHistory>

  @Query(
    value = """
      SELECT distinct bh
      FROM BookingHistory bh
      JOIN BookingHistoryAppointment bha on bh.bookingHistoryId = bha.bookingHistory.bookingHistoryId
      WHERE bha.prisonerNumber = :prisonerNumber AND bh.createdTime BETWEEN :from AND :to
      ORDER BY bh.createdTime ASC
    """,
  )
  fun findBy(prisonerNumber: String, from: LocalDateTime, to: LocalDateTime): List<BookingHistory>
}

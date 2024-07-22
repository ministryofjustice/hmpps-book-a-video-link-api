package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBookingHistory
import java.time.LocalDate
import java.util.stream.Stream

@Repository
interface VideoBookingHistoryRepository : ReadOnlyRepository<VideoBookingHistory, Long> {
  @Query(
    value = """
      FROM VideoBookingHistory vbh 
      WHERE vbh.dateOfBooking >= :fromDate and vbh.dateOfBooking <= :toDate
      ORDER BY vbh.mainDate, vbh.mainStartTime
    """,
  )
  fun findByDateOfBookingBetween(fromDate: LocalDate, toDate: LocalDate): Stream<VideoBookingHistory>

  @Query(
    value = """
      FROM VideoBookingHistory vbh 
      WHERE vbh.mainDate >= :fromDate and vbh.mainDate <= :toDate
      ORDER BY vbh.mainDate, vbh.mainStartTime
    """,
  )
  fun findByMainDateBetween(fromDate: LocalDate, toDate: LocalDate): Stream<VideoBookingHistory>
}

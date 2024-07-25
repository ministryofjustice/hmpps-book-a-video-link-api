package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import jakarta.persistence.QueryHint
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.QueryHints
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBookingEvent
import java.time.LocalDate
import java.util.stream.Stream

@Repository
interface VideoBookingEventRepository : ReadOnlyRepository<VideoBookingEvent, Long> {

  @QueryHints(
    value = [
      QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE, value = "" + Integer.MAX_VALUE),
      QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHEABLE, value = "false"),
      QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_READ_ONLY, value = "true"),
    ],
  )
  @Query(
    value = """
      FROM VideoBookingEvent vbh 
      WHERE vbh.dateOfBooking >= :fromDate and vbh.dateOfBooking <= :toDate
      ORDER BY vbh.mainDate, vbh.mainStartTime
    """,
  )
  fun findByDateOfBookingBetween(fromDate: LocalDate, toDate: LocalDate): Stream<VideoBookingEvent>

  /**
   * The join back onto the view itself is so updates/amends take precedence over creates (cancel/deletes are not
   * wanted here).
   */
  @QueryHints(
    value = [
      QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_FETCH_SIZE, value = "" + Integer.MAX_VALUE),
      QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_CACHEABLE, value = "false"),
      QueryHint(name = org.hibernate.jpa.HibernateHints.HINT_READ_ONLY, value = "true"),
    ],
  )
  @Query(
    value = """
      FROM VideoBookingEvent vbh
      LEFT JOIN VideoBookingEvent later on vbh.videoBookingId = later.videoBookingId and vbh.timestamp < later.timestamp
      WHERE later.videoBookingId is null
      AND   vbh.mainDate >= :fromDate and vbh.mainDate <= :toDate
      AND   vbh.historyType != 'CANCEL'
      ORDER BY vbh.mainDate, vbh.mainStartTime
    """,
  )
  fun findByMainDateBetween(fromDate: LocalDate, toDate: LocalDate): Stream<VideoBookingEvent>
}

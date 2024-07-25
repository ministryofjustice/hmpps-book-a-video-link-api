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
      FROM VideoBookingEvent vbe 
      WHERE vbe.dateOfBooking >= :fromDate and vbe.dateOfBooking <= :toDate
      ORDER BY vbe.mainDate, vbe.mainStartTime
    """,
  )
  fun findByDateOfBookingBetween(fromDate: LocalDate, toDate: LocalDate): Stream<VideoBookingEvent>

  /**
   * The join back onto the view itself is so updates/amends take precedence over creates (cancel/deletes are not
   * wanted here as the booking is no longer live).
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
      FROM VideoBookingEvent vbe
      LEFT JOIN VideoBookingEvent later on vbe.videoBookingId = later.videoBookingId and vbe.timestamp < later.timestamp
      WHERE later.videoBookingId is null
      AND   vbe.mainDate >= :fromDate and vbe.mainDate <= :toDate
      AND   vbe.eventType != 'CANCEL'
      ORDER BY vbe.mainDate, vbe.mainStartTime
    """,
  )
  fun findByMainDateBetween(fromDate: LocalDate, toDate: LocalDate): Stream<VideoBookingEvent>
}

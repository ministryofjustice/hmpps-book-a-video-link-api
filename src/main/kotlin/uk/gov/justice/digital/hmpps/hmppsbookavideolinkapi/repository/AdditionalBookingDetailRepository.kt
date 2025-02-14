package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.AdditionalBookingDetail
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking

interface AdditionalBookingDetailRepository : JpaRepository<AdditionalBookingDetail, Long> {
  fun findByVideoBooking(booking: VideoBooking): AdditionalBookingDetail?
}

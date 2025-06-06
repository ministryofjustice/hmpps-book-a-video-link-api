package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security.checkCaseLoadAccess
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security.checkVideoBookingAccess

/**
 * Service to support the cancellation of future [VideoBooking]'s.
 *
 * [VideoBooking]'s which have already taken place cannot be cancelled.
 */
@Service
class CancelVideoBookingService(
  private val videoBookingRepository: VideoBookingRepository,
  private val bookingHistoryService: BookingHistoryService,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun cancel(videoBookingId: Long, cancelledBy: User): VideoBooking {
    val booking = videoBookingRepository
      .findById(videoBookingId)
      .orElseThrow { EntityNotFoundException("Video booking with ID $videoBookingId not found.") }
      .also { checkVideoBookingAccess(cancelledBy, it) }
      .also { checkCaseLoadAccess(cancelledBy, it.prisonCode()) }

    return booking.cancel(cancelledBy)
      .also { thisBooking -> videoBookingRepository.saveAndFlush(thisBooking) }
      .also { thisBooking -> bookingHistoryService.createBookingHistory(HistoryType.CANCEL, thisBooking) }
      .also { thisBooking -> log.info("CANCELLED BOOKING: Booking ID ${thisBooking.videoBookingId} cancelled") }
  }
}

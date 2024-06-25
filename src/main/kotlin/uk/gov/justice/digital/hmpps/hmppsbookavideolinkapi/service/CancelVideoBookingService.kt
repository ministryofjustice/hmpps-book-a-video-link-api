package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.VideoBookingRepository

/**
 * Service to support the cancellation of future [VideoBooking]'s.
 *
 * [VideoBooking]'s which have already taken place cannot be cancelled.
 */
@Service
class CancelVideoBookingService(
  private val videoBookingRepository: VideoBookingRepository,
) {
  @Transactional
  fun cancel(videoBookingId: Long, cancelledBy: String): VideoBooking {
    val booking = videoBookingRepository
      .findById(videoBookingId)
      .orElseThrow { EntityNotFoundException("Video booking with ID $videoBookingId not found.") }

    return videoBookingRepository.saveAndFlush(booking.cancel(cancelledBy))
  }
}

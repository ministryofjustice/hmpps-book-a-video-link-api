package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest

/**
 * Responsible for delegating video booking specific operations to the appropriate video booking service.
 */
@Service
class VideoBookingServiceDelegate(
  private val createVideoBookingService: CreateVideoBookingService,
  private val createProbationBookingService: CreateProbationBookingService,
  private val amendCourtBookingService: AmendCourtBookingService,
  private val amendProbationBookingService: AmendProbationBookingService,
  private val cancelVideoBookingService: CancelVideoBookingService,
  private val featureSwitches: FeatureSwitches,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun create(booking: CreateVideoBookingRequest, createdBy: User) = when (featureSwitches.isEnabled(Feature.FEATURE_MASTER_VLPM_TYPES)) {
    false -> {
      log.info("CREATE BOOKING DELEGATE: VLPM feature toggle is off.")
      createVideoBookingService.create(booking, createdBy)
    }

    true -> {
      log.info("CREATE BOOKING DELEGATE: VLPM feature toggle is on.")
      when (booking.bookingType!!) {
        BookingType.COURT -> createVideoBookingService.create(booking, createdBy)
        BookingType.PROBATION -> createProbationBookingService.create(booking, createdBy)
      }
    }
  }

  @Transactional
  fun amend(videoBookingId: Long, booking: AmendVideoBookingRequest, amendedBy: User) = when (featureSwitches.isEnabled(Feature.FEATURE_MASTER_VLPM_TYPES)) {
    false -> {
      log.info("AMEND BOOKING DELEGATE: VLPM feature toggle is off.")
      amendCourtBookingService.amend(videoBookingId, booking, amendedBy)
    }

    true -> {
      log.info("AMEND BOOKING DELEGATE: VLPM feature toggle is on.")
      when (booking.bookingType!!) {
        BookingType.COURT -> amendCourtBookingService.amend(videoBookingId, booking, amendedBy)
        BookingType.PROBATION -> amendProbationBookingService.amend(videoBookingId, booking, amendedBy)
      }
    }
  }

  @Transactional
  fun cancel(videoBookingId: Long, cancelledBy: User): VideoBooking = cancelVideoBookingService.cancel(videoBookingId, cancelledBy)
}

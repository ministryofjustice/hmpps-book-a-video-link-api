package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateVideoBookingRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.RequestVideoBookingRequest
import java.time.LocalDate

/**
 * Responsible for delegating video booking specific operations to the appropriate video booking service.
 */
@Service
class VideoBookingServiceDelegate(
  private val createCourtBookingService: CreateCourtBookingService,
  private val createProbationBookingService: CreateProbationBookingService,
  private val amendCourtBookingService: AmendCourtBookingService,
  private val amendProbationBookingService: AmendProbationBookingService,
  private val cancelVideoBookingService: CancelVideoBookingService,
  private val requestBookingService: RequestBookingService,
  @Value("\${applications.max-appointment-start-date-from-today:1000}") private val maxStartDateOffsetDays: Long = 1000,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun create(booking: CreateVideoBookingRequest, createdBy: User) = run {
    log.info("BOOKING DELEGATE: creating video booking: $booking")

    checkStartDateNotTooFarInFuture(booking.prisoners.first().appointments.first().date!!)

    when (booking.bookingType!!) {
      BookingType.COURT -> createCourtBookingService.create(booking, createdBy)
      BookingType.PROBATION -> createProbationBookingService.create(booking, createdBy)
    }
  }

  @Transactional
  fun amend(videoBookingId: Long, booking: AmendVideoBookingRequest, amendedBy: User) = run {
    log.info("BOOKING DELEGATE: amending video booking: $booking")

    checkStartDateNotTooFarInFuture(booking.prisoners.first().appointments.first().date!!)

    when (booking.bookingType!!) {
      BookingType.COURT -> amendCourtBookingService.amend(videoBookingId, booking, amendedBy)
      BookingType.PROBATION -> amendProbationBookingService.amend(videoBookingId, booking, amendedBy)
    }
  }

  @Transactional
  fun cancel(videoBookingId: Long, cancelledBy: User): VideoBooking = run {
    log.info("BOOKING DELEGATE: cancelling video booking: $videoBookingId")

    cancelVideoBookingService.cancel(videoBookingId, cancelledBy)
  }

  fun request(booking: RequestVideoBookingRequest, user: ExternalUser) {
    checkStartDateNotTooFarInFuture(booking.prisoners.first().appointments.first().date!!)

    requestBookingService.request(booking, user)
  }

  private fun checkStartDateNotTooFarInFuture(startDate: LocalDate) {
    if (startDate > LocalDate.now().plusDays(maxStartDateOffsetDays)) {
      throw ValidationException("Start date cannot be more than $maxStartDateOffsetDays days into the future")
    }
  }
}

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType.COURT
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.StatusCode.CANCELLED
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ServiceUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User

class CourtBookingCancelledTelemetryEvent private constructor(
  private val booking: VideoBooking,
  private val cancelledBy: String,
) : MetricTelemetryEvent("BVLS-court-booking-cancelled") {

  init {
    require(booking.isBookingType(COURT)) {
      "Cannot create court cancelled metric, video booking with ID '${booking.videoBookingId}' is not a court booking."
    }

    require(booking.isStatus(CANCELLED)) {
      "Cannot create court cancelled metric, video booking with ID '${booking.videoBookingId}' has not been cancelled."
    }
  }

  override fun properties(): Map<String, String> = booking.commonProperties().plus("cancelled_by" to cancelledBy)

  override fun metrics() = mapOf(booking.hoursBeforeStartTimeMetric())

  companion object {
    fun user(booking: VideoBooking, user: User): CourtBookingCancelledTelemetryEvent {
      require(user is PrisonUser || user is ServiceUser || (user is ExternalUser && user.isCourtUser)) {
        "Can only create court cancelled metric for service, prison or court users."
      }

      require(user.username == booking.amendedBy) {
        "Cannot create court cancelled metric, user does not match the cancelled by user."
      }

      val cancelledBy = when (user) {
        is ExternalUser -> "court"
        is PrisonUser -> "prison"
        is ServiceUser -> "prison"
        else -> throw IllegalArgumentException("Unsupported user type.")
      }

      return CourtBookingCancelledTelemetryEvent(booking, cancelledBy)
    }

    fun released(videoBooking: VideoBooking): CourtBookingCancelledTelemetryEvent = CourtBookingCancelledTelemetryEvent(videoBooking, "release")

    fun transferred(videoBooking: VideoBooking): CourtBookingCancelledTelemetryEvent = CourtBookingCancelledTelemetryEvent(videoBooking, "transfer")
  }
}

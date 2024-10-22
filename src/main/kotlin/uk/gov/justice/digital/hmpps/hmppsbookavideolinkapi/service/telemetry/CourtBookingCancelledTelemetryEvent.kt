package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.StatusCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User

class CourtBookingCancelledTelemetryEvent private constructor(
  private val booking: VideoBooking,
  private val cancelledBy: String,
) : MetricTelemetryEvent("BVLS-court-booking-cancelled") {

  init {
    require(booking.isCourtBooking()) {
      "Cannot create court cancelled metric, video booking with ID '${booking.videoBookingId}' is not a court booking."
    }

    require(booking.isStatus(StatusCode.CANCELLED)) {
      "Cannot create court cancelled metric, video booking with ID '${booking.videoBookingId}' has not been cancelled."
    }
  }

  override fun properties(): Map<String, String> = booking.commonProperties().plus("cancelled_by" to cancelledBy)

  override fun metrics() = mapOf(booking.hoursBeforeStartTimeMetric())

  companion object {
    fun user(booking: VideoBooking, user: User): CourtBookingCancelledTelemetryEvent {
      require(user is PrisonUser || (user is ExternalUser && user.isCourtUser)) {
        "Can only create court cancelled metric for prison or court users."
      }

      require(user.username == booking.amendedBy) {
        "Cannot create court cancelled metric, user does not match the cancelled by user."
      }

      return CourtBookingCancelledTelemetryEvent(booking, if (user is PrisonUser) "prison" else "court")
    }

    fun released(videoBooking: VideoBooking): CourtBookingCancelledTelemetryEvent =
      CourtBookingCancelledTelemetryEvent(videoBooking, "release")

    fun transferred(videoBooking: VideoBooking): CourtBookingCancelledTelemetryEvent =
      CourtBookingCancelledTelemetryEvent(videoBooking, "transfer")
  }
}

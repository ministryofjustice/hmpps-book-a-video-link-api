package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.StatusCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User

class ProbationBookingCancelledTelemetryEvent private constructor(
  private val booking: VideoBooking,
  private val cancelledBy: String,
) : MetricTelemetryEvent("BVLS-probation-booking-cancelled") {

  init {
    require(booking.isProbationBooking()) {
      "Cannot create probation cancelled metric, video booking with ID '${booking.videoBookingId}' is not a probation booking."
    }

    require(booking.isStatus(StatusCode.CANCELLED)) {
      "Cannot create probation cancelled metric, video booking with ID '${booking.videoBookingId}' has not been cancelled."
    }
  }

  override fun properties(): Map<String, String> = booking.commonProperties().plus("cancelled_by" to cancelledBy)

  override fun metrics() = mapOf(booking.hoursBeforeStartTimeMetric())

  companion object {
    fun user(booking: VideoBooking, user: User): ProbationBookingCancelledTelemetryEvent {
      require(user is PrisonUser || (user is ExternalUser && user.isProbationUser)) {
        "Cannot only create probation cancelled metric for prison or probation users."
      }

      require(user.username == booking.amendedBy) {
        "Cannot create probation cancelled metric, user does not match the cancelled by user."
      }

      return ProbationBookingCancelledTelemetryEvent(booking, if (user is PrisonUser) "prison" else "probation")
    }

    fun released(videoBooking: VideoBooking): ProbationBookingCancelledTelemetryEvent =
      ProbationBookingCancelledTelemetryEvent(videoBooking, "release")

    fun transferred(videoBooking: VideoBooking): ProbationBookingCancelledTelemetryEvent =
      ProbationBookingCancelledTelemetryEvent(videoBooking, "transfer")
  }
}

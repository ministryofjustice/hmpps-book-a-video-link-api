package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType.PROBATION
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.StatusCode.CANCELLED
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.DeliusUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ServiceUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User

class ProbationBookingCancelledTelemetryEvent private constructor(
  private val booking: VideoBooking,
  private val cancelledBy: String,
) : MetricTelemetryEvent("BVLS-probation-booking-cancelled") {

  init {
    require(booking.isBookingType(PROBATION)) {
      "Cannot create probation cancelled metric, video booking with ID '${booking.videoBookingId}' is not a probation booking."
    }

    require(booking.isStatus(CANCELLED)) {
      "Cannot create probation cancelled metric, video booking with ID '${booking.videoBookingId}' has not been cancelled."
    }
  }

  override fun properties(): Map<String, String> = booking
    .commonProperties()
    .plus("cancelled_by" to cancelledBy.substringBefore(":"))
    .plus("delius_user" to if (cancelledBy.contains(":delius")) "true" else "false")

  override fun metrics() = mapOf(booking.hoursBeforeStartTimeMetric())

  companion object {
    fun user(booking: VideoBooking, user: User): ProbationBookingCancelledTelemetryEvent {
      require(user is PrisonUser || user is ServiceUser || user is DeliusUser || (user is ExternalUser && user.isProbationUser)) {
        "Can only create probation cancelled metric for service, prison, delius or probation users."
      }

      require(user.username == booking.amendedBy) {
        "Cannot create probation cancelled metric, user does not match the cancelled by user."
      }

      val cancelledBy = when (user) {
        is ExternalUser -> "probation"
        is DeliusUser -> "probation:delius"
        else -> "prison"
      }

      return ProbationBookingCancelledTelemetryEvent(booking, cancelledBy)
    }

    fun released(videoBooking: VideoBooking): ProbationBookingCancelledTelemetryEvent = ProbationBookingCancelledTelemetryEvent(videoBooking, "release")

    fun transferred(videoBooking: VideoBooking): ProbationBookingCancelledTelemetryEvent = ProbationBookingCancelledTelemetryEvent(videoBooking, "transfer")
  }
}

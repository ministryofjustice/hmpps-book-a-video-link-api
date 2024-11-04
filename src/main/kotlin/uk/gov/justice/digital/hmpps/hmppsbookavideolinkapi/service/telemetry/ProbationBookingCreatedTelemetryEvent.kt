package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking

class ProbationBookingCreatedTelemetryEvent(private val booking: VideoBooking) :
  MetricTelemetryEvent("BVLS-probation-booking-created") {

  init {
    require(booking.isProbationBooking()) {
      "Cannot create probation created metric, video booking with ID '${booking.videoBookingId}' is not a probation booking."
    }

    require(booking.amendedTime == null) {
      "Cannot create probation created metric, video booking with ID '${booking.videoBookingId}' has been amended."
    }
  }

  // Probation bookings are only created by probation users
  override fun properties() = booking.commonProperties().plus("created_by" to "probation")

  override fun metrics() = mapOf(booking.hoursBeforeStartTimeMetric())
}

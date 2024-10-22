package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking

class CourtBookingCreatedTelemetryEvent(private val booking: VideoBooking) :
  MetricTelemetryEvent("BVLS-court-booking-created") {

  init {
    require(booking.isCourtBooking()) {
      "Cannot create court created metric, video booking with ID '${booking.videoBookingId}' is not a court booking."
    }

    require(booking.amendedTime == null) {
      "Cannot create court created metric, video booking with ID '${booking.videoBookingId}' has been amended."
    }
  }

  override fun properties() =
    booking.commonProperties().plus("created_by" to if (booking.createdByPrison) "prison" else "court")

  override fun metrics() = mapOf(booking.hoursBeforeStartTimeMetric())
}

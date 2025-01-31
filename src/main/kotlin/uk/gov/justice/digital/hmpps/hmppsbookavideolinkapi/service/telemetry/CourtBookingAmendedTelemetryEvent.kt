package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType.COURT
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User

class CourtBookingAmendedTelemetryEvent(private val booking: VideoBooking, private val amendedBy: User) :
  MetricTelemetryEvent("BVLS-court-booking-amended") {

  init {
    require(booking.isBookingType(COURT)) {
      "Cannot create court amended metric, video booking with ID '${booking.videoBookingId}' is not a court booking."
    }

    require(booking.amendedTime != null) {
      "Cannot create court amended metric, video booking with ID '${booking.videoBookingId}' has not been amended."
    }

    require(booking.amendedBy == amendedBy.username) {
      "Cannot create court amended metric, user does not match the amended by user."
    }
  }

  override fun properties() =
    booking.commonProperties().plus("amended_by" to if (amendedBy is PrisonUser) "prison" else "court")

  override fun metrics() = mapOf(booking.hoursBeforeStartTimeMetric())
}

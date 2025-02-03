package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType.PROBATION
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User

class ProbationBookingAmendedTelemetryEvent(private val booking: VideoBooking, private val amendedBy: User) : MetricTelemetryEvent("BVLS-probation-booking-amended") {

  init {
    require(booking.isBookingType(PROBATION)) {
      "Cannot create probation amended metric, video booking with ID '${booking.videoBookingId}' is not a probation booking."
    }

    require(booking.amendedTime != null) {
      "Cannot create probation amended metric, video booking with ID '${booking.videoBookingId}' has not been amended."
    }

    require(booking.amendedBy == amendedBy.username) {
      "Cannot create probation amended metric, user does not match the amended by user."
    }
  }

  override fun properties() = booking.commonProperties().plus("amended_by" to if (amendedBy is PrisonUser) "prison" else "probation")

  override fun metrics() = mapOf(booking.hoursBeforeStartTimeMetric())
}

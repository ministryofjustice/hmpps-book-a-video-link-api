package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoDateTime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import java.time.temporal.ChronoUnit.HOURS

class ProbationBookingAmendedTelemetryEvent(private val booking: VideoBooking, private val amendedByPrisonUser: Boolean) :
  MetricTelemetryEvent("BVLS-probation-booking-amended") {

  init {
    require(booking.isProbationBooking()) {
      "Cannot create probation amended metric, video booking with ID '${booking.videoBookingId}' is not a probation booking."
    }

    require(booking.amendedTime != null) {
      "Cannot create probation amended metric, video booking with ID '${booking.videoBookingId}' has not been amended."
    }
  }

  override fun properties() =
    mapOf(
      "video_booking_id" to "${booking.videoBookingId}",
      "amended_by" to if (amendedByPrisonUser) "prison" else "probation",
      "team_code" to booking.probationTeam!!.code,
      "meeting_type" to booking.probationMeetingType!!,
      "prison_code" to booking.prisonCode(),
      "location_key" to booking.appointment().prisonLocKey,
      "start" to booking.appointment().start().toIsoDateTime(),
      "end" to booking.appointment().end().toIsoDateTime(),
      "cvp_link" to (booking.videoUrl != null).toString(),
    )

  override fun metrics() =
    mapOf("hoursBeforeStartTime" to HOURS.between(booking.amendedTime!!, booking.appointment().start()).toDouble())

  private fun VideoBooking.appointment() = appointments().single { it.isType("VLB_PROBATION") }
}

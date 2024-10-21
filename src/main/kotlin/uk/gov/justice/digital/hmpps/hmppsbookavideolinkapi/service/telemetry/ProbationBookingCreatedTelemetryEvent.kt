package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoDateTime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import java.time.temporal.ChronoUnit

class ProbationBookingCreatedTelemetryEvent(private val booking: VideoBooking) :
  MetricTelemetryEvent("BVLS-probation-booking-created") {

  init {
    require(booking.isProbationBooking()) {
      "Cannot create probation created metric, video booking with ID '${booking.videoBookingId}' is not a probation booking."
    }
  }

  override fun properties() =
    mapOf(
      "video_booking_id" to "${booking.videoBookingId}",
      "team_code" to booking.probationTeam!!.code,
      "meeting_type" to booking.probationMeetingType!!,
      "prison_code" to booking.prisonCode(),
      "location_key" to booking.appointment().prisonLocKey,
      "start" to booking.appointment().start().toIsoDateTime(),
      "end" to booking.appointment().end().toIsoDateTime(),
      "cvp_link" to (booking.videoUrl != null).toString(),
    )

  override fun metrics() =
    mapOf(
      "hoursBeforeStartTime" to ChronoUnit.HOURS.between(booking.createdTime, booking.appointment().start()).toDouble(),
    )

  private fun VideoBooking.appointment() = appointments().single { it.isType("VLB_PROBATION") }
}

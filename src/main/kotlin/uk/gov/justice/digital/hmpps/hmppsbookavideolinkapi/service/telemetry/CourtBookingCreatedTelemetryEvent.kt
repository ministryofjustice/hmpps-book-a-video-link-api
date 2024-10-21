package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoDateTime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import java.time.temporal.ChronoUnit

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
    mapOf(
      "video_booking_id" to "${booking.videoBookingId}",
      "created_by" to if (booking.createdByPrison) "prison" else "court",
      "court_code" to booking.court!!.code,
      "hearing_type" to booking.hearingType!!,
      "prison_code" to booking.prisonCode(),
      "pre_location_key" to (booking.pre()?.prisonLocKey ?: ""),
      "pre_start" to (booking.pre()?.start()?.toIsoDateTime() ?: ""),
      "pre_end" to (booking.pre()?.end()?.toIsoDateTime() ?: ""),
      "main_location_key" to booking.main().prisonLocKey,
      "main_start" to booking.main().start().toIsoDateTime(),
      "main_end" to booking.main().end().toIsoDateTime(),
      "post_location_key" to (booking.post()?.prisonLocKey ?: ""),
      "post_start" to (booking.post()?.start()?.toIsoDateTime() ?: ""),
      "post_end" to (booking.post()?.end()?.toIsoDateTime() ?: ""),
      "cvp_link" to (booking.videoUrl != null).toString(),
    )

  override fun metrics() =
    mapOf(
      "hoursBeforeStartTime" to ChronoUnit.HOURS.between(booking.createdTime, booking.main().start()).toDouble(),
    )

  private fun VideoBooking.pre() = appointments().singleOrNull { it.isType("VLB_COURT_PRE") }

  private fun VideoBooking.main() = appointments().single { it.isType("VLB_COURT_MAIN") }

  private fun VideoBooking.post() = appointments().singleOrNull { it.isType("VLB_COURT_POST") }
}
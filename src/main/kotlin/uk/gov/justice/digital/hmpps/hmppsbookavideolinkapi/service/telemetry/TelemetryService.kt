package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoDateTime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingType.COURT
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import java.time.temporal.ChronoUnit

@Service
class TelemetryService(private val telemetryClient: TelemetryClient) {
  fun track(event: TelemetryEvent) {
    when (event) {
      is MetricTelemetryEvent -> telemetryClient.trackEvent(event.eventType, event.properties(), event.metrics())
      is StandardTelemetryEvent -> telemetryClient.trackEvent(event.eventType, event.properties(), null)
    }
  }
}

sealed class TelemetryEvent(val eventType: String) {
  abstract fun properties(): Map<String, String>

  protected fun VideoBooking.pre() = appointments().singleOrNull { it.isType("VLB_COURT_PRE") }

  protected fun VideoBooking.main() = appointments().single { it.isType("VLB_COURT_MAIN") }

  protected fun VideoBooking.post() = appointments().singleOrNull { it.isType("VLB_COURT_POST") }

  protected fun VideoBooking.meeting() = appointments().single { it.isType("VLB_PROBATION") }

  protected fun VideoBooking.commonProperties(): Map<String, String> = if (isBookingType(COURT)) {
    mapOf(
      "video_booking_id" to "$videoBookingId",
      "court_code" to court!!.code,
      "hearing_type" to hearingType!!,
      "prison_code" to prisonCode(),
      "pre_location_id" to (pre()?.prisonLocationId?.toString() ?: ""),
      "pre_start" to (pre()?.start()?.toIsoDateTime() ?: ""),
      "pre_end" to (pre()?.end()?.toIsoDateTime() ?: ""),
      "main_location_id" to main().prisonLocationId.toString(),
      "main_start" to main().start().toIsoDateTime(),
      "main_end" to main().end().toIsoDateTime(),
      "post_location_id" to (post()?.prisonLocationId?.toString() ?: ""),
      "post_start" to (post()?.start()?.toIsoDateTime() ?: ""),
      "post_end" to (post()?.end()?.toIsoDateTime() ?: ""),
      "cvp_link" to (videoUrl != null || hmctsNumber != null).toString(),
      "guest_pin" to (guestPin != null).toString(),
    )
  } else {
    mapOf(
      "video_booking_id" to "$videoBookingId",
      "team_code" to probationTeam!!.code,
      "meeting_type" to probationMeetingType!!,
      "prison_code" to prisonCode(),
      "location_id" to meeting().prisonLocationId.toString(),
      "start" to meeting().start().toIsoDateTime(),
      "end" to meeting().end().toIsoDateTime(),
    )
  }
}

abstract class MetricTelemetryEvent(eventType: String) : TelemetryEvent(eventType) {
  abstract fun metrics(): Map<String, Double>

  protected fun VideoBooking.hoursBeforeStartTimeMetric(): Pair<String, Double> = if (isBookingType(COURT)) {
    "hoursBeforeStartTime" to ChronoUnit.HOURS.between(amendedTime ?: createdTime, main().start()).toDouble()
  } else {
    "hoursBeforeStartTime" to ChronoUnit.HOURS.between(amendedTime ?: createdTime, meeting().start()).toDouble()
  }
}

abstract class StandardTelemetryEvent(eventType: String) : TelemetryEvent(eventType)

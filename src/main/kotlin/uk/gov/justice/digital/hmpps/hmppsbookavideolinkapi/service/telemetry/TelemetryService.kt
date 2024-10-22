package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoDateTime
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

  protected fun VideoBooking.commonProperties(): Map<String, String> =
    if (isCourtBooking()) {
      mapOf(
        "video_booking_id" to "$videoBookingId",
        "court_code" to court!!.code,
        "hearing_type" to hearingType!!,
        "prison_code" to prisonCode(),
        "pre_location_key" to (pre()?.prisonLocKey ?: ""),
        "pre_start" to (pre()?.start()?.toIsoDateTime() ?: ""),
        "pre_end" to (pre()?.end()?.toIsoDateTime() ?: ""),
        "main_location_key" to main().prisonLocKey,
        "main_start" to main().start().toIsoDateTime(),
        "main_end" to main().end().toIsoDateTime(),
        "post_location_key" to (post()?.prisonLocKey ?: ""),
        "post_start" to (post()?.start()?.toIsoDateTime() ?: ""),
        "post_end" to (post()?.end()?.toIsoDateTime() ?: ""),
        "cvp_link" to (videoUrl != null).toString(),
      )
    } else {
      mapOf(
        "video_booking_id" to "$videoBookingId",
        "team_code" to probationTeam!!.code,
        "meeting_type" to probationMeetingType!!,
        "prison_code" to prisonCode(),
        "location_key" to meeting().prisonLocKey,
        "start" to meeting().start().toIsoDateTime(),
        "end" to meeting().end().toIsoDateTime(),
        "cvp_link" to (videoUrl != null).toString(),
      )
    }
}

abstract class MetricTelemetryEvent(eventType: String) : TelemetryEvent(eventType) {
  abstract fun metrics(): Map<String, Double>

  protected fun VideoBooking.hoursBeforeStartTimeMetric(): Pair<String, Double> {
    return if (isCourtBooking()) {
      "hoursBeforeStartTime" to ChronoUnit.HOURS.between(amendedTime ?: createdTime, main().start()).toDouble()
    } else {
      "hoursBeforeStartTime" to ChronoUnit.HOURS.between(amendedTime ?: createdTime, meeting().start()).toDouble()
    }
  }
}

abstract class StandardTelemetryEvent(eventType: String) : TelemetryEvent(eventType)

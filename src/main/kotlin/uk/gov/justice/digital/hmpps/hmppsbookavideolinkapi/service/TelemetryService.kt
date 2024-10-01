package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.stereotype.Service

@Service
class TelemetryService(private val telemetryClient: TelemetryClient) {

  fun track(event: TelemetryEvent) {
    telemetryClient.trackEvent(event.eventType.label, event.properties(), null)
  }
}

abstract class TelemetryEvent(val eventType: TelemetryEventType) {
  abstract fun properties(): Map<String, String>
}

enum class TelemetryEventType(val label: String) {
  MIGRATED_BOOKING_FAILURE("BVLS-migrated-booking-failure"),
  MIGRATED_BOOKING_SUCCESS("BVLS-migrated-booking-success"),
  JOB_FAILURE("BVLS-job-failure"),
  JOB_SUCCESS("BVLS-job-success"),
}

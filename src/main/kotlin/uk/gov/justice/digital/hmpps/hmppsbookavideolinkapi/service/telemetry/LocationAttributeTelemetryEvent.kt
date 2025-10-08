package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoDate
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationAttribute
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ServiceUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User

class LocationAttributeTelemetryEvent(private val attribute: LocationAttribute, private val raisedBy: User) : StandardTelemetryEvent("BVLS-room-status-event") {
  init {
    if (attribute.amendedTime == null) {
      require(attribute.createdBy == raisedBy.username) {
        "Cannot create location status telemetry event, created by user does not match raised by user."
      }
    } else {
      require(attribute.amendedBy == raisedBy.username) {
        "Cannot create location status telemetry event, amended by user does not match raised by user."
      }
    }
  }

  override fun properties(): Map<String, String> = run {
    buildMap {
      put("location_attribute_id", attribute.locationAttributeId.toString())
      put("prison_code", attribute.prison.code)
      put("dps_location_id", attribute.dpsLocationId.toString())
      put("location_status", attribute.locationStatus.name)
      put("location_usage", attribute.locationUsage.name)

      if (attribute.locationStatus == LocationStatus.TEMPORARILY_BLOCKED) {
        put("blocked_from", (attribute.blockedFrom?.toIsoDate() ?: ""))
        put("blocked_to", (attribute.blockedTo?.toIsoDate() ?: ""))
      }

      if (attribute.amendedBy == null) {
        put("created_by", if (raisedBy is ServiceUser) "service" else "user")
      } else {
        put("amended_by", if (raisedBy is ServiceUser) "service" else "user")
      }
    }
  }
}

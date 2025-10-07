package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoDate
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationAttribute
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.COURT_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.RISLEY
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.SERVICE_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsEntriesExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvillePrison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.risleyPrison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthPrison
import java.util.UUID

class LocationAttributeTelemetryEventTest {

  private val activeLocationAttribute = LocationAttribute.decoratedRoom(
    dpsLocationId = UUID.fromString("e58ed763-928c-4155-bee9-aaaaaaaaaaaa"),
    prison = risleyPrison,
    locationUsage = LocationUsage.SHARED,
    allowedParties = emptySet(),
    locationStatus = LocationStatus.ACTIVE,
    prisonVideoUrl = null,
    notes = null,
    createdBy = COURT_USER,
  )

  private val inactiveLocationAttribute = LocationAttribute.decoratedRoom(
    dpsLocationId = UUID.fromString("e58ed763-928c-4155-bee9-bbbbbbbbbbbb"),
    prison = wandsworthPrison,
    locationUsage = LocationUsage.SHARED,
    allowedParties = emptySet(),
    locationStatus = LocationStatus.INACTIVE,
    prisonVideoUrl = null,
    notes = null,
    createdBy = COURT_USER,
  )

  private val blockedLocationAttribute = LocationAttribute.decoratedRoom(
    dpsLocationId = UUID.fromString("e58ed763-928c-4155-bee9-cccccccccccc"),
    prison = pentonvillePrison,
    locationUsage = LocationUsage.SHARED,
    allowedParties = emptySet(),
    locationStatus = LocationStatus.TEMPORARILY_BLOCKED,
    blockedFrom = today(),
    blockedTo = tomorrow(),
    prisonVideoUrl = null,
    notes = null,
    createdBy = COURT_USER,
  )

  @Test
  fun `should capture custom event for active location attribute`() {
    val properties = LocationAttributeTelemetryEvent(activeLocationAttribute, COURT_USER).properties()

    properties containsEntriesExactlyInAnyOrder mapOf(
      "location_attribute_id" to activeLocationAttribute.locationAttributeId.toString(),
      "dps_location_id" to UUID.fromString("e58ed763-928c-4155-bee9-aaaaaaaaaaaa").toString(),
      "prison_code" to RISLEY,
      "location_status" to LocationStatus.ACTIVE.name,
      "created_by" to "user",
    )
  }

  @Test
  fun `should capture amended custom event from active to inactive location attribute`() {
    val inactive = LocationAttribute.amend(
      locationAttributeToAmend = activeLocationAttribute,
      locationStatus = LocationStatus.INACTIVE,
      locationUsage = LocationUsage.SHARED,
      allowedParties = emptySet(),
      prisonVideoUrl = null,
      comments = null,
      amendedBy = COURT_USER,
    )

    val properties = LocationAttributeTelemetryEvent(inactive, COURT_USER).properties()

    properties containsEntriesExactlyInAnyOrder mapOf(
      "location_attribute_id" to inactive.locationAttributeId.toString(),
      "dps_location_id" to UUID.fromString("e58ed763-928c-4155-bee9-aaaaaaaaaaaa").toString(),
      "prison_code" to RISLEY,
      "location_status" to LocationStatus.INACTIVE.name,
      "amended_by" to "user",
    )
  }

  @Test
  fun `should capture amended custom event from active to blocked location attribute`() {
    val blocked = LocationAttribute.amend(
      locationAttributeToAmend = activeLocationAttribute,
      locationStatus = LocationStatus.TEMPORARILY_BLOCKED,
      locationUsage = LocationUsage.SHARED,
      allowedParties = emptySet(),
      prisonVideoUrl = null,
      comments = null,
      blockedFrom = today(),
      blockedTo = tomorrow(),
      amendedBy = COURT_USER,
    )

    val properties = LocationAttributeTelemetryEvent(blocked, COURT_USER).properties()

    properties containsEntriesExactlyInAnyOrder mapOf(
      "location_attribute_id" to blocked.locationAttributeId.toString(),
      "dps_location_id" to UUID.fromString("e58ed763-928c-4155-bee9-aaaaaaaaaaaa").toString(),
      "prison_code" to RISLEY,
      "location_status" to LocationStatus.TEMPORARILY_BLOCKED.name,
      "blocked_from" to today().toIsoDate(),
      "blocked_to" to tomorrow().toIsoDate(),
      "amended_by" to "user",
    )
  }

  @Test
  fun `should capture custom event for inactive location attribute`() {
    val properties = LocationAttributeTelemetryEvent(inactiveLocationAttribute, COURT_USER).properties()

    properties containsEntriesExactlyInAnyOrder mapOf(
      "location_attribute_id" to activeLocationAttribute.locationAttributeId.toString(),
      "dps_location_id" to UUID.fromString("e58ed763-928c-4155-bee9-bbbbbbbbbbbb").toString(),
      "prison_code" to WANDSWORTH,
      "location_status" to LocationStatus.INACTIVE.name,
      "created_by" to "user",
    )
  }

  @Test
  fun `should capture custom event for blocked location attribute`() {
    val properties = LocationAttributeTelemetryEvent(blockedLocationAttribute, COURT_USER).properties()

    properties containsEntriesExactlyInAnyOrder mapOf(
      "location_attribute_id" to blockedLocationAttribute.locationAttributeId.toString(),
      "dps_location_id" to UUID.fromString("e58ed763-928c-4155-bee9-cccccccccccc").toString(),
      "prison_code" to PENTONVILLE,
      "location_status" to LocationStatus.TEMPORARILY_BLOCKED.name,
      "blocked_from" to today().toIsoDate(),
      "blocked_to" to tomorrow().toIsoDate(),
      "created_by" to "user",
    )
  }

  @Test
  fun `should capture amended custom event from blocked to active location attribute`() {
    val active = LocationAttribute.reactivate(blockedLocationAttribute, SERVICE_USER)

    val properties = LocationAttributeTelemetryEvent(active, SERVICE_USER).properties()

    properties containsEntriesExactlyInAnyOrder mapOf(
      "location_attribute_id" to blockedLocationAttribute.locationAttributeId.toString(),
      "dps_location_id" to UUID.fromString("e58ed763-928c-4155-bee9-cccccccccccc").toString(),
      "prison_code" to PENTONVILLE,
      "location_status" to LocationStatus.ACTIVE.name,
      "amended_by" to "service",
    )
  }
}

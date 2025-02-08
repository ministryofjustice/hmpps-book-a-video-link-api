package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationAttribute
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationSchedule
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.RoomAttributes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.RoomSchedule
import java.time.DayOfWeek
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location as ModelLocation

// Undecorated locations
fun Location.toModel() = ModelLocation(
  key = key,
  description = localName,
  enabled = active,
  dpsLocationId = id,
)

// Undecorated locations
fun List<Location>.toModel() = map { it.toModel() }.sortedBy { it.description }

// Decorated locations
fun ModelLocation.toDecoratedLocation(attributes: RoomAttributes) = ModelLocation(
  key = key,
  description = description,
  enabled = enabled,
  dpsLocationId = dpsLocationId,
  extraAttributes = attributes,
)

fun LocationAttribute.toRoomAttributes() = RoomAttributes(
  attributeId = this.locationAttributeId,
  locationStatus = this.locationStatus,
  statusMessage = this.statusMessage,
  expectedActiveDate = this.expectedActiveDate,
  locationUsage = this.locationUsage,
  allowedParties = this.allowedParties.let { this.allowedParties?.split(",") } ?: emptyList(),
  prisonVideoUrl = this.prisonVideoUrl,
  notes = this.notes,
  schedule = this.schedule().toRoomSchedule(),
)

fun List<LocationSchedule>.toRoomSchedule() = map {
  RoomSchedule(
    scheduleId = it.locationScheduleId,
    startDayOfWeek = DayOfWeek.of(it.startDayOfWeek),
    endDayOfWeek = DayOfWeek.of(it.endDayOfWeek),
    startTime = it.startTime,
    endTime = it.endTime,
    locationUsage = it.locationUsage,
    allowedParties = it.allowedParties.let { parties -> parties?.split(",") } ?: emptyList(),
  )
}

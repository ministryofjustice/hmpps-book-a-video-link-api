package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationAttribute
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationSchedule
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.RoomAttributes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.RoomSchedule
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location as ModelLocation

fun Location.toModel() = ModelLocation(
  key = key,
  description = localName,
  enabled = active,
)

fun List<Location>.toModel() = map { it.toModel() }.sortedBy { it.description }

fun ModelLocation.toDecoratedLocation(attributes: RoomAttributes) = ModelLocation(
  key = key,
  description = description,
  enabled = enabled,
  extraAttributes = attributes,
)

fun LocationAttribute.toRoomAttributes() = RoomAttributes(
  locationStatus = this.locationStatus,
  statusMessage = this.statusMessage,
  expectedActiveDate = this.expectedActiveDate,
  locationUsage = this.locationUsage,
  allowedParties = this.allowedParties,
  prisonVideoUrl = this.prisonVideoUrl,
  schedule = this.locationSchedule.toRoomSchedule(),
)

fun List<LocationSchedule>.toRoomSchedule() = map {
  RoomSchedule(
    startDayOfWeek = it.startDayOfWeek,
    endDayOfWeek = it.endDayOfWeek,
    startTime = it.startTime,
    endTime = it.endTime,
    locationUsage = it.locationUsage,
    allowedParties = it.allowedParties,
  )
}

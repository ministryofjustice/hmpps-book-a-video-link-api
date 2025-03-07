package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationAttribute
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationSchedule
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationScheduleUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.RoomAttributes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.RoomSchedule
import java.time.DayOfWeek
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location as ModelLocation

fun Location.toModel(attributes: RoomAttributes? = null) = ModelLocation(
  key = key,
  prisonCode = prisonId,
  description = localName,
  enabled = active,
  dpsLocationId = id,
  extraAttributes = attributes,
)

fun List<Location>.toModel(attributes: List<LocationAttribute>) = map {
  val attributesForLocation = attributes.singleOrNull { a -> a.dpsLocationId == it.id }
  it.toModel(attributesForLocation?.toRoomAttributes())
}.sortedBy { it.description }

fun LocationAttribute.toRoomAttributes() = RoomAttributes(
  attributeId = this.locationAttributeId,
  locationStatus = LocationStatus.valueOf(this.locationStatus.name),
  statusMessage = this.statusMessage,
  expectedActiveDate = this.expectedActiveDate,
  locationUsage = LocationUsage.valueOf(this.locationUsage.name),
  allowedParties = this.allowedParties.let { this.allowedParties?.split(",") } ?: emptyList(),
  prisonVideoUrl = this.prisonVideoUrl,
  notes = this.notes,
  schedule = this.schedule().toRoomSchedule(),
)

fun List<LocationSchedule>.toRoomSchedule() = map { it.toModel() }

fun LocationSchedule.toModel() = RoomSchedule(
  scheduleId = locationScheduleId,
  startDayOfWeek = DayOfWeek.of(startDayOfWeek),
  endDayOfWeek = DayOfWeek.of(endDayOfWeek),
  startTime = startTime,
  endTime = endTime,
  locationUsage = LocationScheduleUsage.valueOf(locationUsage.name),
  allowedParties = allowedParties.let { parties -> parties?.split(",") } ?: emptyList(),
)

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.availability

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.PrisonRegime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TimeSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.AvailabilityStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.TimeSlotAvailabilityRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AvailableLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AvailableLocationsResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.slot
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService
import java.time.LocalTime

/**
 * This provides a snapshot (time slot) of available rooms at time of calling. Note this does not guarantee the
 * rooms can be booked, by the time the user attempts to save the booking the room could already have been taken by
 * another user of the service.
 *
 * This service works on the principle of the single room model. For example, it won't provide times for pre/main/post
 * court hearings at different locations, the assumption is they would all be in the same room.
 */
@Service
@Transactional(readOnly = true)
class TimeSlotAvailabilityService(
  locationsService: LocationsService,
  private val bookedLocationsService: BookedLocationsService,
  private val prisonRegime: PrisonRegime,
  private val timeSource: TimeSource,
  private val locationAttributesService: LocationAttributesAvailableService,
) : LocationAvailabilityService<TimeSlotAvailabilityRequest>(locationsService) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun findAvailable(request: TimeSlotAvailabilityRequest): AvailableLocationsResponse {
    val (startOfDay, endOfDay) = getStartAndEndOfDay(request)
    val prisonVideoLinkLocations = getVideoLinkLocationsAt(request.prisonCode!!)
    val bookedLocations = bookedLocationsService.findBooked(BookedLookup(request.prisonCode, request.date!!, prisonVideoLinkLocations, request.vlbIdToExclude))
    val meetingDuration = request.bookingDuration!!.toLong()

    val availableLocationsBuilder = TimeSlotLocationsBuilder.builder {
      prisonVideoLinkLocations.forEach { location ->
        // These time adjustments do not allow for PRE and POST meeting times.
        var meetingStartTime = startOfDay
        var meetingEndTime = meetingStartTime.plusMinutes(meetingDuration)

        while (meetingStartTime.isBefore(endOfDay)) {
          if (!bookedLocations.isBooked(location, meetingStartTime, meetingEndTime) && request.fallsWithinSlotTime(meetingStartTime)) {
            add(
              availabilityStatus = location.allowsByAnyRuleOrSchedule(request, meetingStartTime),
              availableLocation = AvailableLocation(
                name = location.description ?: location.key,
                startTime = meetingStartTime,
                endTime = meetingEndTime,
                dpsLocationId = location.dpsLocationId,
                dpsLocationKey = location.key,
                usage = location.extraAttributes?.locationUsage?.let { LocationUsage.valueOf(it.name) } ?: LocationUsage.SHARED,
                timeSlot = slot(meetingStartTime),
              ),
            )
          }

          meetingStartTime = meetingStartTime.plusMinutes(15)
          meetingEndTime = meetingEndTime.plusMinutes(15)
        }
      }
    }

    return AvailableLocationsResponse(
      availableLocationsBuilder
        .build()
        .also { log.info("TIME SLOT AVAILABLE LOCATIONS: found ${it.size} available locations matching request $request") },
    )
  }

  private fun getStartAndEndOfDay(request: TimeSlotAvailabilityRequest): Pair<LocalTime, LocalTime> {
    val regimeStartOfDay = prisonRegime.startOfDay(request.prisonCode!!)
    val regimeEndOfDay = prisonRegime.endOfDay(request.prisonCode)
    val now = timeSource.now()

    // Start looking for meeting 15 mins from now if looking for slots today when the time has passed the regime start time.
    if (request.isForToday() && regimeStartOfDay.isBefore(now.toLocalTime())) {
      return when (now.minute) {
        0 -> LocalTime.of(now.hour, 15)
        in 1..15 -> LocalTime.of(now.hour, 30)
        in 16..30 -> LocalTime.of(now.hour, 45)
        else -> LocalTime.of(now.hour + 1, 0)
      } to regimeEndOfDay
    }

    return regimeStartOfDay to regimeEndOfDay
  }

  private fun TimeSlotAvailabilityRequest.fallsWithinSlotTime(time: LocalTime) = timeSlots == null || timeSlots.any { slot -> slot.isTimeInSlot(time) }

  private fun TimeSlotAvailabilityRequest.isForToday() = this.date!! == timeSource.today()

  private fun Location.allowsByAnyRuleOrSchedule(request: TimeSlotAvailabilityRequest, time: LocalTime): AvailabilityStatus {
    if (extraAttributes != null) {
      return when (request.bookingType!!) {
        BookingType.COURT -> locationAttributesService.isLocationAvailableFor(
          LocationAvailableRequest.court(
            extraAttributes.attributeId,
            request.courtCode!!,
            request.date!!.atTime(time),
          ),
        )
        BookingType.PROBATION -> locationAttributesService.isLocationAvailableFor(
          LocationAvailableRequest.probation(
            extraAttributes.attributeId,
            request.probationTeamCode!!,
            request.date!!.atTime(time),
          ),
        )
      }
    }

    return AvailabilityStatus.SHARED
  }
}

class TimeSlotLocationsBuilder private constructor() : LocationAvailabilityService.AvailableLocationBuilder() {
  companion object {
    fun builder(init: TimeSlotLocationsBuilder.() -> Unit) = TimeSlotLocationsBuilder().also { it.init() }
  }

  fun build() = run {
    val filter: (Collection<AvailableLocation>, AvailableLocation) -> Boolean =
      { locations, location -> locations.none { it.startTime == location.startTime } }

    val probation = buildList {
      addAll(dedicatedProbationTeamLocations)
      addAll(anyProbationTeamLocations.filter { that -> filter(this, that) })
    }

    val court = buildList {
      addAll(dedicatedCourtLocations)
      addAll(anyCourtLocations.filter { that -> filter(this, that) })
    }

    if (probation.isNotEmpty() && court.isNotEmpty()) {
      throw IllegalStateException("Cannot mix probation and court locations")
    }

    buildList {
      addAll(probation)
      addAll(court)
      addAll(sharedLocations.filter { that -> filter(this, that) })
    }.sortedWith(compareBy({ it.startTime }, { it.name })).distinctBy { it.startTime }
  }
}

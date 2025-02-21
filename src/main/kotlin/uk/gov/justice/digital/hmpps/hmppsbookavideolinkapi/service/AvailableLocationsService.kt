package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.PrisonRegime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AvailableLocationsRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.slot
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AvailableLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AvailableLocationsResponse

private const val FIFTEEN_MINUTES = 15L

@Service
@Transactional(readOnly = true)
class AvailableLocationsService(
  private val locationsService: LocationsService,
  private val bookedLocationsService: BookedLocationsService,
  private val prisonRegime: PrisonRegime,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  /**
   * This provides a snapshot (point in time) of available rooms at time of calling. Note this does not guarantee the
   * room can be booked, by the time the user attempts to save the booking the room could already have been taken by
   * another user of the service.
   */
  fun findAvailableLocations(request: AvailableLocationsRequest, maxSlots: Int = 10): AvailableLocationsResponse {
    require(maxSlots > 0) {
      "The cap for the maximum number of available slots must be a positive number"
    }

    val startOfDay = prisonRegime.startOfDay(request.prisonCode!!)
    val endOfDay = prisonRegime.endOfDay(request.prisonCode)
    val prisonVideoLinkLocations = getDecoratedLocationsAt(request.prisonCode)
    val bookedLocations = bookedLocationsService.findBooked(BookedLookup(request.prisonCode, request.date!!, prisonVideoLinkLocations, request.vlbIdToExclude))
    val meetingDuration = request.bookingDuration!!.toLong()

    val availableLocations = buildList {
      prisonVideoLinkLocations.forEach { location ->
        // These time adjustments do not allow for PRE and POST meeting times.
        var meetingStartTime = startOfDay
        var meetingEndTime = meetingStartTime.plusMinutes(meetingDuration)

        while (meetingStartTime.isBefore(endOfDay) && this.size < maxSlots) {
          if (bookedLocations.isBooked(location, meetingStartTime, meetingEndTime).not() && request.timeSlots!!.any { slot -> slot.isTimeInSlot(meetingStartTime) }) {
            // TODO need to check against the room decoration/schedules
            add(
              AvailableLocation(
                // TODO is it possible for the description to be null?
                name = location.description ?: "UNKNOWN",
                startTime = meetingStartTime,
                endTime = meetingEndTime,
                dpsLocationId = location.dpsLocationId,
                dpsLocationKey = location.key,
                // TODO populate usage
                usage = null,
                timeSlot = slot(meetingStartTime),
              ),
            )
          }

          meetingStartTime = meetingStartTime.plusMinutes(FIFTEEN_MINUTES)
          meetingEndTime = meetingEndTime.plusMinutes(FIFTEEN_MINUTES)
        }
      }
    }

    return AvailableLocationsResponse(availableLocations).also { log.info("AVAILABLE LOCATIONS: found ${it.locations.size} available locations matching request $request") }
  }

  private fun getDecoratedLocationsAt(prisonCode: String) = locationsService.getDecoratedVideoLocations(prisonCode = prisonCode, enabledOnly = true)
}

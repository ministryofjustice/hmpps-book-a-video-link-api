package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.availability

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.TimeSource
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.AvailabilityStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.BookingType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.DateTimeAvailabilityRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AvailableLocationsResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService
import java.time.LocalTime

/**
 * This provides a snapshot (date and time) of available rooms at time of calling. Note this does not guarantee the
 * rooms can be booked, by the time the user attempts to save the booking the room could already have been taken by
 * another user of the service.
 */
@Service
@Transactional
class DateTimeAvailabilityService(
  locationsService: LocationsService,
  private val bookedLocationsService: BookedLocationsService,
  private val locationAttributesService: LocationAttributesAvailableService,
  private val timeSource: TimeSource,
) : LocationAvailabilityService<DateTimeAvailabilityRequest>(locationsService) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  override fun findAvailable(request: DateTimeAvailabilityRequest): AvailableLocationsResponse {
    require(request.date!!.atTime(request.startTime!!).isAfter(timeSource.now())) {
      "Requested date and start time must be in the future."
    }

    val prisonVideoLinkLocations = getVideoLinkLocationsAt(request.prisonCode!!)
    val bookedLocations = bookedLocationsService.findBooked(BookedLookup(request.prisonCode, request.date, prisonVideoLinkLocations, request.vlbIdToExclude))

    val availableLocationsBuilder = AvailableLocationsBuilder.availableLocations {
      prisonVideoLinkLocations.forEach { location ->
        val meetingStartTime = request.startTime
        val meetingEndTime = request.endTime!!

        if (!bookedLocations.isBooked(location, meetingStartTime, meetingEndTime)) {
          val availabilityStatus = location.allowsByAnyRuleOrSchedule(request, meetingStartTime)
          add(availabilityStatus, location, meetingStartTime, meetingEndTime)
        }
      }
    }

    return AvailableLocationsResponse(
      availableLocationsBuilder
        .build()
        .also { log.info("DATE TIME AVAILABLE LOCATIONS: found ${it.size} available locations matching request $request") },
    )
  }

  protected fun Location.allowsByAnyRuleOrSchedule(request: DateTimeAvailabilityRequest, time: LocalTime): AvailabilityStatus {
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

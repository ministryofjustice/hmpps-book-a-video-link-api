package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.ActivitiesAppointmentsClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.nomismapping.NomisMappingClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.isTimesOverlap
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.PrisonRegime
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AvailableLocationsRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AvailableLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AvailableLocationsResponse
import java.time.LocalDate
import java.time.LocalTime

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
   * This provides a snapshot (point in time) of available rooms at time of calling. When the user attempts to save the
   * booking the room could have already been taken so may fail.
   */
  fun findAvailableLocations(request: AvailableLocationsRequest): AvailableLocationsResponse {
    val startOfDay = prisonRegime.startOfDay(request.prisonCode!!)
    val endOfDay = prisonRegime.endOfDay(request.prisonCode)
    val prisonVideoLinkLocations = getDecoratedLocationsAt(request.prisonCode)
    val bookedLocations = bookedLocationsService.findBooked(request.prisonCode, request.date!!, prisonVideoLinkLocations)
    val duration = request.bookingDuration!!.toLong()

    val availableLocations = buildList {
      prisonVideoLinkLocations.forEach { location ->
        // These time adjustments do not allow for PRE and POST meeting times.
        var startTime = startOfDay
        var endTime = startOfDay.plusMinutes(duration)

        while (startTime.isBefore(endOfDay)) {
          if (bookedLocations.isBooked(location, startTime, endTime).not()) {
            // TODO check start and end time falls with the supplied slot times as well e.g. AM/PM/ED
            // TODO need to check against the room decoration/schedules
            add(
              AvailableLocation(
                // TODO is it possible for the description to be null?
                name = location.description ?: "UNKNOWN",
                startTime = startTime,
                endTime = endTime,
                dpsLocationId = location.dpsLocationId,
                dpsLocationKey = location.key,
                // TODO populate usage
                usage = null,
              ),
            )
          }

          startTime = startTime.plusMinutes(15)
          endTime = endTime.plusMinutes(15)
        }
      }
    }

    return AvailableLocationsResponse(availableLocations.toList())
  }

  private fun getDecoratedLocationsAt(prisonCode: String) = locationsService.getDecoratedVideoLocations(prisonCode = prisonCode, enabledOnly = true)
}

@Service
@Transactional(readOnly = true)
class BookedLocationsService(
  private val activitiesAppointmentsClient: ActivitiesAppointmentsClient,
  private val prisonApiClient: PrisonApiClient,
  private val nomisMappingClient: NomisMappingClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun findBooked(prisonCode: String, date: LocalDate, locations: Collection<Location>): BookedLocations {
    val nomisToDpsLocations = nomisMappingClient.getNomisLocationMappingsBy(locations.map(Location::dpsLocationId))
      .associate { it.nomisLocationId to it.dpsLocationId }

    return when (activitiesAppointmentsClient.isAppointmentsRolledOutAt(prisonCode)) {
      true -> activitiesAppointmentsClient.getScheduledAppointments(prisonCode, date, nomisToDpsLocations.keys)
        .map { result ->
          BookedLocation(
            locations.single { l -> l.dpsLocationId == nomisToDpsLocations[result.internalLocation!!.id] },
            startTime = LocalTime.parse(result.startTime),
            endTime = LocalTime.parse(result.endTime),
          )
        }
        .let { BookedLocations(it) }

      false -> prisonApiClient.getScheduledAppointments(prisonCode, date, nomisToDpsLocations.keys)
        .map { result ->
          BookedLocation(
            locations.single { l -> l.dpsLocationId == nomisToDpsLocations[result.locationId] },
            startTime = result.startTime.toLocalTime(),
            endTime = result.endTime!!.toLocalTime(),
          )
        }
        .let { BookedLocations(it) }
    }
  }
}

data class BookedLocation(val location: Location, val startTime: LocalTime, val endTime: LocalTime)

class BookedLocations(booked: Collection<BookedLocation>) {

  private val locations = booked.groupBy { it.location.key }

  fun isBooked(location: Location, startTime: LocalTime, endTime: LocalTime) = locations[location.key]?.any { isTimesOverlap(it.startTime, it.endTime, startTime, endTime) } ?: false
}

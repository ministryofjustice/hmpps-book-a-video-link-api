package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.ActivitiesAppointmentsClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.nomismapping.NomisMappingClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.isTimesOverlap
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import java.time.LocalDate
import java.time.LocalTime

@Service
@Transactional(readOnly = true)
class BookedLocationsService(
  private val activitiesAppointmentsClient: ActivitiesAppointmentsClient,
  private val prisonApiClient: PrisonApiClient,
  private val nomisMappingClient: NomisMappingClient,
) {
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

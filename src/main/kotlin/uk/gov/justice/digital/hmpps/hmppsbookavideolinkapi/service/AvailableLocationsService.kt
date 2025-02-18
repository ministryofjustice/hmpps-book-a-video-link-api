package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.ActivitiesAppointmentsClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.nomismapping.NomisMappingClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.isOnOrBefore
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.isTimesOverlap
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AvailableLocationsRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AvailableLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AvailableLocationsResponse
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Service
@Transactional(readOnly = true)
class AvailableLocationsService(
  private val locationsService: LocationsService,
  private val bookedLocationsService: BookedLocationsService,
) {
  // TODO hardcoded start and end of days values, needs to go into config.
  private val startOfDay = LocalTime.of(8, 0)
  private val endOfDay = LocalTime.of(18, 0)

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun findAvailableLocationsAndTimes(request: AvailableLocationsRequest): AvailableLocationsResponse = runBlocking {
    val prisonVideoLinkLocations = getAllVideoLocationsAtPrison(request.prisonCode!!).associateBy { it.dpsLocationId }
    val bookedLocations = bookedLocationsService.lookup(request.prisonCode, request.date!!, prisonVideoLinkLocations.keys)

    val availableLocations = buildList {
      val duration = request.bookingDuration!!.toLong()

      prisonVideoLinkLocations.forEach {
        // These time adjustments do not allow for PRE and POST meeting times.
        var startTime = startOfDay
        var endTime = startOfDay.plusMinutes(duration)

        while (endTime.isOnOrBefore(endOfDay)) {
          if (bookedLocations.isLocationFreeAt(it.key, startTime, endTime)) {
            add(
              AvailableLocation(
                // TODO is it possible for the description to be null?
                name = it.value.description ?: "UNKNOWN",
                startTime = startTime,
                endTime = endTime,
                dpsLocationId = it.key,
                dpsLocationKey = it.value.key,
                usage = null,
              ),
            )
          }
          startTime = startTime.plusMinutes(duration)
          endTime = endTime.plusMinutes(duration)
        }
      }
    }

    AvailableLocationsResponse(availableLocations.toList())
  }

  private fun getAllVideoLocationsAtPrison(prisonCode: String) = locationsService.getDecoratedVideoLocations(prisonCode = prisonCode, enabledOnly = true)
}

@Service
class BookedLocationsService(
  private val activitiesAppointmentsClient: ActivitiesAppointmentsClient,
  private val prisonApiClient: PrisonApiClient,
  private val nomisMappingClient: NomisMappingClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  suspend fun lookup(prisonCode: String, date: LocalDate, locations: Set<UUID>): BookedLocations {
    val locationIds = locations.mapNotNull { nomisMappingClient.getNomisLocationMappingBy(it) }
      .associate { it.nomisLocationId to it.dpsLocationId }

    return when (activitiesAppointmentsClient.isAppointmentsRolledOutAt(prisonCode)) {
      true -> activitiesAppointmentsClient.getScheduledAppointments(prisonCode, date, locationIds.keys)
        .groupBy(
          { locationIds[it.internalLocation!!.id]!! },
          {
            BookedLocation(
              locationIds[it.internalLocation!!.id]!!,
              LocalTime.parse(it.startTime),
              LocalTime.parse(it.endTime),
            )
          },
        )
        .let { BookedLocations(it) }

      false -> prisonApiClient.getScheduledAppointments(prisonCode, date, locationIds.keys)
        .groupBy(
          { locationIds[it.locationId]!! },
          { BookedLocation(locationIds[it.locationId]!!, it.startTime.toLocalTime(), it.endTime!!.toLocalTime()) },
        )
        .let { BookedLocations(it) }
    }
  }

  data class BookedLocation(val locationId: UUID, val startTime: LocalTime, val endTime: LocalTime)

  data class BookedLocations(private val booked: Map<UUID, List<BookedLocation>>) {
    fun isLocationFreeAt(locationId: UUID, startTime: LocalTime, endTime: LocalTime) = if (booked.containsKey(locationId)) {
      booked[locationId]!!.none { isTimesOverlap(it.startTime, it.endTime, startTime, endTime) }
    } else {
      true
    }
  }
}

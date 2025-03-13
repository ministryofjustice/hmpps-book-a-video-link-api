package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.availability

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.AvailabilityStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AvailableLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AvailableLocationsResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.slot
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService
import java.time.LocalTime

abstract class LocationAvailabilityService<REQUEST>(private val locationsService: LocationsService) {

  abstract fun findAvailable(request: REQUEST): AvailableLocationsResponse

  protected fun getVideoLinkLocationsAt(prisonCode: String) = locationsService.getVideoLinkLocationsAtPrison(prisonCode = prisonCode, enabledOnly = true)

  protected class AvailableLocationsBuilder private constructor() {
    companion object {
      fun availableLocations(init: AvailableLocationsBuilder.() -> Unit) = AvailableLocationsBuilder().also { it.init() }
    }

    private val dedicatedProbationTeamLocations = mutableSetOf<AvailableLocation>()
    private val anyProbationTeamLocations = mutableSetOf<AvailableLocation>()
    private val dedicatedCourtLocations = mutableSetOf<AvailableLocation>()
    private val anyCourtLocations = mutableSetOf<AvailableLocation>()
    private val sharedLocations = mutableSetOf<AvailableLocation>()

    fun add(availabilityStatus: AvailabilityStatus, location: Location, startTime: LocalTime, endTime: LocalTime) {
      AvailableLocation(
        name = location.description ?: location.key,
        startTime = startTime,
        endTime = endTime,
        dpsLocationId = location.dpsLocationId,
        dpsLocationKey = location.key,
        usage = location.extraAttributes?.locationUsage?.let { LocationUsage.valueOf(it.name) } ?: LocationUsage.SHARED,
        timeSlot = slot(startTime),
      ).let { availableLocation ->
        when (availabilityStatus) {
          AvailabilityStatus.PROBATION_ROOM -> dedicatedProbationTeamLocations.add(availableLocation)
          AvailabilityStatus.PROBATION_ANY -> anyProbationTeamLocations.add(availableLocation)
          AvailabilityStatus.COURT_ROOM -> dedicatedCourtLocations.add(availableLocation)
          AvailabilityStatus.COURT_ANY -> anyCourtLocations.add(availableLocation)
          AvailabilityStatus.SHARED -> sharedLocations.add(availableLocation)
          AvailabilityStatus.NONE -> {}
        }
      }
    }

    fun buildDistinct() = build(true).distinctBy { it.startTime }

    fun build() = build(false)

    private fun build(distinct: Boolean) = run {
      val probation = buildList<AvailableLocation> {
        addAll(dedicatedProbationTeamLocations)

        if (distinct) {
          addAll(anyProbationTeamLocations.filter { any -> this.none { it.startTime == any.startTime } })
        } else {
          addAll(anyProbationTeamLocations)
        }
      }

      // This is not complete for courts (regardless of schedules), it is more complicated than this with pre and post meetings!!!!
      val court = buildList<AvailableLocation> {
        addAll(dedicatedCourtLocations)

        if (distinct) {
          addAll(anyCourtLocations.filter { any -> this.none { it.startTime == any.startTime } })
        } else {
          addAll(anyCourtLocations)
        }
      }

      if (probation.isNotEmpty() && court.isNotEmpty()) {
        throw IllegalStateException("Cannot mix probation and court locations")
      }

      val availableLocations = buildList<AvailableLocation> {
        addAll(probation)
        addAll(court)

        if (distinct) {
          addAll(sharedLocations.filter { shared -> this.none { it.startTime == shared.startTime } })
        } else {
          addAll(sharedLocations)
        }
      }

      availableLocations.sortedWith(compareBy({ it.startTime }, { it.name })).toList()
    }
  }
}

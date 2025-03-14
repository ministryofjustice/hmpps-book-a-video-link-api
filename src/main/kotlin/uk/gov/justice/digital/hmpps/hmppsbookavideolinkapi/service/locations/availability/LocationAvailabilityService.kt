package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.availability

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.AvailabilityStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AvailableLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.AvailableLocationsResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService

abstract class LocationAvailabilityService<REQUEST>(private val locationsService: LocationsService) {

  abstract fun findAvailable(request: REQUEST): AvailableLocationsResponse

  protected fun getVideoLinkLocationsAt(prisonCode: String) = locationsService.getVideoLinkLocationsAtPrison(prisonCode = prisonCode, enabledOnly = true)

  abstract class AvailableLocationBuilder {
    protected val dedicatedProbationTeamLocations = mutableSetOf<AvailableLocation>()
    protected val anyProbationTeamLocations = mutableSetOf<AvailableLocation>()
    protected val dedicatedCourtLocations = mutableSetOf<AvailableLocation>()
    protected val anyCourtLocations = mutableSetOf<AvailableLocation>()
    protected val sharedLocations = mutableSetOf<AvailableLocation>()

    fun add(availabilityStatus: AvailabilityStatus, availableLocation: AvailableLocation) {
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
}

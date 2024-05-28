package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel

@Service
class LocationsService(
  private val prisonRepository: PrisonRepository,
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient,
) {
  fun getLocationsAtPrison(prisonCode: String, enabledOnly: Boolean) =
    prisonRepository.findByCode(prisonCode)
      ?.let { locationsInsidePrisonClient.getLocationsAtPrison(prisonCode).filter { !enabledOnly || it.active }.map(Location::toModel) } ?: emptyList()
}

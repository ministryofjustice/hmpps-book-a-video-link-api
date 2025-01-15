package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.LocationAttributeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toDecoratedLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toRoomAttributes

@Service
class LocationsService(
  private val prisonRepository: PrisonRepository,
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient,
  private val locationAttributeRepository: LocationAttributeRepository,
) {
  fun getNonResidentialLocationsAtPrison(prisonCode: String, enabledOnly: Boolean) =
    prisonRepository.findByCode(prisonCode)
      ?.let { locationsInsidePrisonClient.getNonResidentialAppointmentLocationsAtPrison(prisonCode) }
      ?.filter { !enabledOnly || it.active }
      ?.toModel() ?: emptyList()

  fun getVideoLinkLocationsAtPrison(prisonCode: String, enabledOnly: Boolean) =
    prisonRepository.findByCode(prisonCode)
      ?.let { locationsInsidePrisonClient.getVideoLinkLocationsAtPrison(prisonCode) }
      ?.filter { !enabledOnly || it.active }
      ?.toModel() ?: emptyList()

  fun getDecoratedVideoLocations(prisonCode: String, enabledOnly: Boolean): List<Location> {
    val prisonLocations = prisonRepository.findByCode(prisonCode)
      ?.let { locationsInsidePrisonClient.getVideoLinkLocationsAtPrison(prisonCode) }
      ?.filter { !enabledOnly || it.active }
      ?.toModel() ?: emptyList()

    val locationsById = prisonLocations.associateBy { it.key }

    return locationAttributeRepository.findByPrisonCode(prisonCode)
      .filter { locationsById[it.locationKey] != null }
      .mapNotNull { attributes ->
        locationsById[attributes.locationKey]?.toDecoratedLocation(attributes.toRoomAttributes())
      }
  }
}

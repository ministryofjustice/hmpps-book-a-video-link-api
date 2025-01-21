package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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

  @Transactional(readOnly = true)
  fun getDecoratedVideoLocations(prisonCode: String, enabledOnly: Boolean): List<Location> {
    val prisonLocations = getVideoLinkLocationsAtPrison(prisonCode, enabledOnly)
    val locationsById = prisonLocations.associateBy { it.dpsLocationId }

    val decoratedLocations = locationAttributeRepository.findByPrisonCode(prisonCode)
      .filter { locationsById[it.dpsLocationId] != null }
      .mapNotNull { attributes ->
        locationsById[attributes.dpsLocationId]?.toDecoratedLocation(attributes.toRoomAttributes())
      }

    return decoratedLocations.ifEmpty {
      prisonLocations
    }
  }
}

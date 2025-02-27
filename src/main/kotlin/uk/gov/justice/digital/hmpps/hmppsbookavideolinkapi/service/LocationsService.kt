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
import java.util.UUID

@Service
class LocationsService(
  private val prisonRepository: PrisonRepository,
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient,
  private val locationAttributeRepository: LocationAttributeRepository,
) {
  fun getNonResidentialLocationsAtPrison(prisonCode: String, enabledOnly: Boolean) = prisonRepository.findByCode(prisonCode)
    ?.let { locationsInsidePrisonClient.getNonResidentialAppointmentLocationsAtPrison(prisonCode) }
    ?.filter { !enabledOnly || it.active }
    ?.toModel() ?: emptyList()

  fun getVideoLinkLocationsAtPrison(prisonCode: String, enabledOnly: Boolean) = prisonRepository.findByCode(prisonCode)
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
    // Preserves the order of the original prison locations
    return (locationsById + decoratedLocations.associateBy { it.dpsLocationId }).values.toList()
  }

  /**
   * Will also include room attributes if there are any.
   */
  fun getLocationById(id: UUID) = locationsInsidePrisonClient.getLocationById(id)
    ?.let { location ->
      locationAttributeRepository.findByDpsLocationId(id)
        ?.let { attributes ->
          location.toModel().toDecoratedLocation(attributes.toRoomAttributes())
        } ?: location.toModel()
    }

  /**
   * Will also include room attributes if there are any.
   */
  fun getLocationByKey(key: String) = locationsInsidePrisonClient.getLocationByKey(key)
    ?.let { location ->
      locationAttributeRepository.findByDpsLocationId(location.id)
        ?.let { attributes ->
          location.toModel().toDecoratedLocation(attributes.toRoomAttributes())
        } ?: location.toModel()
    }
}

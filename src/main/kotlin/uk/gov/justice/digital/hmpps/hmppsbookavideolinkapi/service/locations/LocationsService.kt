package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.LocationAttributeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toRoomAttributes
import java.util.UUID

@Service
@Transactional(readOnly = true)
class LocationsService(
  private val prisonRepository: PrisonRepository,
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient,
  private val locationAttributeRepository: LocationAttributeRepository,
) {
  fun getNonResidentialLocationsAtPrison(prisonCode: String, enabledOnly: Boolean) = prisonRepository.findByCode(prisonCode)
    ?.let { locationsInsidePrisonClient.getNonResidentialAppointmentLocationsAtPrison(prisonCode) }
    ?.filter { !enabledOnly || it.active }
    ?.toModel(locationAttributeRepository.findByPrisonCode(prisonCode)) ?: emptyList()

  fun getVideoLinkLocationsAtPrison(prisonCode: String, enabledOnly: Boolean) = prisonRepository.findByCode(prisonCode)
    ?.let { locationsInsidePrisonClient.getVideoLinkLocationsAtPrison(prisonCode) }
    ?.filter { !enabledOnly || it.active }
    ?.toModel(locationAttributeRepository.findByPrisonCode(prisonCode)) ?: emptyList()

  /**
   * Will also include room attributes if there are any.
   */
  fun getLocationById(id: UUID) = locationsInsidePrisonClient.getLocationById(id)
    ?.let { location -> locationAttributeRepository.findByDpsLocationId(id).let { attributes -> location.toModel(attributes?.toRoomAttributes()) } }

  /**
   * Will also include room attributes if there are any.
   */
  fun getLocationByKey(key: String) = locationsInsidePrisonClient.getLocationByKey(key)
    ?.let { location -> locationAttributeRepository.findByDpsLocationId(location.id).let { attributes -> location.toModel(attributes?.toRoomAttributes()) } }
}

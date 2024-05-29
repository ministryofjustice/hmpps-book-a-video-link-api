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
  fun getNonResidentialLocationsAtPrison(prisonCode: String, enabledOnly: Boolean) =
    prisonRepository.findByCode(prisonCode)
      ?.let { locationsInsidePrisonClient.getNonResidentialAppointmentLocationsAtPrison(prisonCode) }
      ?.filter { !enabledOnly || it.active }
      ?.map(Location::toModel) ?: emptyList()

  fun getVideoLinkLocationsAtPrison(prisonCode: String, enabledOnly: Boolean) =
    prisonRepository.findByCode(prisonCode)
      ?.let { locationsInsidePrisonClient.getVideoLinkLocationsAtPrison(prisonCode) }
      ?.filter { !enabledOnly || it.active }
      ?.map(Location::toModel) ?: emptyList()
}

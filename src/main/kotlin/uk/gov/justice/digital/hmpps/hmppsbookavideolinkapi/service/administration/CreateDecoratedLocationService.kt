package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.administration

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationAttribute
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateDecoratedRoomRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.LocationAttributeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.LocationsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toRoomAttributes
import java.util.UUID

@Service
class CreateDecoratedLocationService(
  private val locationAttributeRepository: LocationAttributeRepository,
  private val locationsService: LocationsService,
  private val prisonRepository: PrisonRepository,
) {
  @Transactional
  fun create(dpsLocationId: UUID, request: CreateDecoratedRoomRequest, createdBy: ExternalUser): Location {
    val location = locationsService.getLocationById(dpsLocationId) ?: throw EntityNotFoundException("DPS location with ID $dpsLocationId not found.")

    require(location.extraAttributes == null) {
      "DPS location with ID $dpsLocationId is already decorated."
    }

    return locationAttributeRepository.saveAndFlush(
      LocationAttribute.decoratedRoom(
        dpsLocationId = dpsLocationId,
        prison = prisonRepository.findByCode(location.prisonCode) ?: throw EntityNotFoundException("Matching prison code ${location.prisonCode} not found for DPS location ID $dpsLocationId."),
        locationStatus = LocationStatus.valueOf(request.locationStatus!!.name),
        locationUsage = LocationUsage.valueOf(request.locationUsage!!.name),
        allowedParties = request.allowedParties ?: emptySet(),
        prisonVideoUrl = request.prisonVideoUrl,
        notes = request.comments,
        createdBy = createdBy,
      ),
    ).let { location.copy(extraAttributes = it.toRoomAttributes()) }
  }
}

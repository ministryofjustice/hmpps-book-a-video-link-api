package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.administration

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendDecoratedRoomRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.LocationAttributeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.LocationsService
import java.util.UUID

@Service
@Transactional
class AmendDecoratedLocationService(
  private val locationsService: LocationsService,
  private val locationAttributeRepository: LocationAttributeRepository,
) {
  fun amend(dpsLocationId: UUID, request: AmendDecoratedRoomRequest, user: ExternalUser): Location {
    val decoratedRoom = locationAttributeRepository.findByDpsLocationId(dpsLocationId)
      ?: throw EntityNotFoundException("Existing room decoration for DPS location ID $dpsLocationId not found.")

    locationAttributeRepository.saveAndFlush(
      decoratedRoom.amend(
        locationUsage = LocationUsage.valueOf(request.locationUsage!!.name),
        locationStatus = LocationStatus.valueOf(request.locationStatus!!.name),
        prisonVideoUrl = request.prisonVideoUrl,
        allowedParties = request.allowedParties ?: emptySet(),
        comments = request.comments,
        amendedBy = user,
      ),
    )

    return locationsService.getLocationById(dpsLocationId) ?: throw EntityNotFoundException("DPS location with ID $dpsLocationId not found.")
  }
}

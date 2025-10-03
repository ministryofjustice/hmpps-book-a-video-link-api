package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.administration

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationAttribute
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationScheduleUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendDecoratedRoomRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendRoomScheduleRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.LocationAttributeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.LocationScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import java.util.UUID

@Service
@Transactional
class AmendDecoratedLocationService(
  private val locationsService: LocationsService,
  private val locationAttributeRepository: LocationAttributeRepository,
  private val locationScheduleRepository: LocationScheduleRepository,
) {
  fun amend(dpsLocationId: UUID, request: AmendDecoratedRoomRequest, amendedBy: ExternalUser): Location {
    val decoratedRoom = locationAttributeRepository.findByDpsLocationId(dpsLocationId)
      ?: throw EntityNotFoundException("Existing room decoration for DPS location ID $dpsLocationId not found.")

    locationAttributeRepository.saveAndFlush(
      LocationAttribute.amend(
        decoratedRoom,
        locationUsage = LocationUsage.valueOf(request.locationUsage!!.name),
        locationStatus = LocationStatus.valueOf(request.locationStatus!!.name),
        prisonVideoUrl = request.prisonVideoUrl,
        allowedParties = request.allowedParties ?: emptySet(),
        comments = request.comments,
        amendedBy = amendedBy,
        blockedFrom = request.blockedFrom,
        blockedTo = request.blockedTo,
      ),
    )

    return locationsService.getLocationById(dpsLocationId) ?: throw EntityNotFoundException("DPS location with ID $dpsLocationId not found.")
  }

  fun amend(dpsLocationId: UUID, scheduleId: Long, request: AmendRoomScheduleRequest, amendedBy: ExternalUser) = run {
    val schedule = locationScheduleRepository.findByScheduleIdAndDpsLocationId(scheduleId, dpsLocationId)
      ?: throw EntityNotFoundException("Location schedule ID $scheduleId not found for DPS location ID $dpsLocationId")

    locationScheduleRepository.saveAndFlush(
      schedule.amend(
        locationUsage = LocationScheduleUsage.valueOf(request.locationUsage!!.name),
        startDayOfWeek = request.startDayOfWeek!!,
        endDayOfWeek = request.endDayOfWeek!!,
        startTime = request.startTime!!,
        endTime = request.endTime!!,
        allowedParties = request.allowedParties,
        notes = request.notes,
        amendedBy = amendedBy,
      ),
    ).toModel()
  }
}

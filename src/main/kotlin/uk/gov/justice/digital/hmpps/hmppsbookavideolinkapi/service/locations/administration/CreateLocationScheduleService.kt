package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.administration

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationScheduleUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationUsage.SCHEDULE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateRoomScheduleRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.LocationAttributeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import java.util.UUID

@Service
class CreateLocationScheduleService(
  private val locationAttributeRepository: LocationAttributeRepository,
) {
  @Transactional
  fun create(dpsLocationId: UUID, request: CreateRoomScheduleRequest, createdBy: ExternalUser) = run {
    val locationAttribute = locationAttributeRepository.findByDpsLocationId(dpsLocationId)
      ?: throw EntityNotFoundException("Location attribute with DPS location ID $dpsLocationId not found")

    require(locationAttribute.isLocationUsage(SCHEDULE)) {
      "Location attribute with ID ${locationAttribute.locationAttributeId} requires a location usage of SCHEDULE to add a row to it."
    }

    locationAttribute.addSchedule(
      usage = LocationScheduleUsage.valueOf(request.locationUsage!!.name),
      startDayOfWeek = request.startDayOfWeek!!,
      endDayOfWeek = request.endDayOfWeek!!,
      startTime = request.startTime!!,
      endTime = request.endTime!!,
      allowedParties = request.allowedParties.takeUnless { it.isNullOrEmpty() } ?: emptySet(),
      notes = request.notes,
      createdBy = createdBy,
    )

    locationAttributeRepository.saveAndFlush(locationAttribute).schedule().last().toModel()
  }
}

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.administration

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationUsage.SCHEDULE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateRoomScheduleRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.LocationAttributeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import java.util.UUID

@Service
class CreateRoomScheduleService(
  private val locationAttributeRepository: LocationAttributeRepository,
) {
  @Transactional
  fun create(dpsLocationId: UUID, request: CreateRoomScheduleRequest, user: ExternalUser) {
    val locationAttribute = locationAttributeRepository.findByDpsLocationId(dpsLocationId)
      ?: throw EntityNotFoundException("Location attribute with DPS location ID $dpsLocationId not found")

    locationAttribute.run {
      require(isLocationUsage(SCHEDULE)) {
        "Location attribute with ID $locationAttributeId requires a location usage of SCHEDULE to add a row to it."
      }

      addSchedule(
        usage = LocationUsage.valueOf(request.locationUsage!!.name),
        startDayOfWeek = request.startDayOfWeek!!,
        endDayOfWeek = request.endDayOfWeek!!,
        startTime = request.startTime!!,
        endTime = request.endTime!!,
        allowedParties = request.allowedParties.takeUnless { it.isNullOrEmpty() } ?: emptySet(),
        notes = request.notes,
        createdBy = user.username,
      )

      locationAttributeRepository.saveAndFlush(this)
    }
  }
}

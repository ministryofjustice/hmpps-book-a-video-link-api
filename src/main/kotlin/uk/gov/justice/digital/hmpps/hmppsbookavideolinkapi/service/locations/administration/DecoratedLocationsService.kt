package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.administration

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationAttribute
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendDecoratedRoomRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.AmendRoomScheduleRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateDecoratedRoomRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.CreateRoomScheduleRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.LocationAttributeRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.LocationScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.ExternalUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.LocationAttributeTelemetryEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.telemetry.TelemetryService
import java.time.LocalDate
import java.util.UUID

@Service
@Transactional
class DecoratedLocationsService(
  private val createDecoratedLocationService: CreateDecoratedLocationService,
  private val amendDecoratedLocationService: AmendDecoratedLocationService,
  private val createLocationScheduleService: CreateLocationScheduleService,
  private val locationAttributeRepository: LocationAttributeRepository,
  private val locationScheduleRepository: LocationScheduleRepository,
  private val telemetryService: TelemetryService,
) {
  fun decorateLocation(dpsLocationId: UUID, request: CreateDecoratedRoomRequest, createdBy: ExternalUser) = run {
    createDecoratedLocationService.create(dpsLocationId, request, createdBy)
  }

  fun amendDecoratedLocation(dpsLocationId: UUID, request: AmendDecoratedRoomRequest, amendedBy: ExternalUser) = run {
    amendDecoratedLocationService.amend(dpsLocationId, request, amendedBy)
  }

  fun deleteDecoratedLocation(dpsLocationId: UUID) {
    locationAttributeRepository.deleteLocationAttributesByDpsLocationId(dpsLocationId)
  }

  fun createSchedule(dpsLocationId: UUID, request: CreateRoomScheduleRequest, createdBy: ExternalUser) = run {
    createLocationScheduleService.create(dpsLocationId, request, createdBy)
  }

  fun amendSchedule(dpsLocationId: UUID, scheduleId: Long, request: AmendRoomScheduleRequest, amendedBy: ExternalUser) = run {
    amendDecoratedLocationService.amend(dpsLocationId, scheduleId, request, amendedBy)
  }

  fun deleteSchedule(dpsLocationId: UUID, scheduleId: Long) {
    locationScheduleRepository.deleteSchedule(scheduleId, dpsLocationId)
  }

  fun reactivateBlockedLocationsBefore(beforeDate: LocalDate) = run {
    locationAttributeRepository
      .findByBlockedToNotNullAndBlockedToIsBefore(beforeDate)
      .onEach(::reactivate)
      .toMutableList()
      .let(locationAttributeRepository::saveAllAndFlush)
      .forEach(::trackReactivation)
  }

  private fun reactivate(locationAttribute: LocationAttribute) {
    LocationAttribute.reactivate(locationAttribute, UserService.getServiceAsUser())
  }

  private fun trackReactivation(locationAttribute: LocationAttribute) {
    telemetryService.track(LocationAttributeTelemetryEvent(locationAttribute, UserService.getServiceAsUser()))
  }
}

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.ScheduleItem
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.locations.LocationsService
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ScheduleItem as ScheduleItemEntity

@Service
@Transactional(readOnly = true)
class ScheduleService(
  private val scheduleRepository: ScheduleRepository,
  private val locationsService: LocationsService,
) {
  fun getScheduleForPrison(prisonCode: String, date: LocalDate, includeCancelled: Boolean): List<ScheduleItem> = if (includeCancelled) {
    scheduleRepository.getScheduleForPrisonIncludingCancelled(prisonCode, date).mapScheduleToModel()
  } else {
    scheduleRepository.getScheduleForPrison(prisonCode, date).mapScheduleToModel()
  }

  fun getScheduleForCourt(courtCode: String, date: LocalDate, includeCancelled: Boolean): List<ScheduleItem> = if (includeCancelled) {
    scheduleRepository.getScheduleForCourtIncludingCancelled(courtCode, date).mapScheduleToModel()
  } else {
    scheduleRepository.getScheduleForCourt(courtCode, date).mapScheduleToModel()
  }

  fun getScheduleForProbationTeam(probationTeamCode: String, date: LocalDate, includeCancelled: Boolean): List<ScheduleItem> = if (includeCancelled) {
    scheduleRepository.getScheduleForProbationTeamIncludingCancelled(probationTeamCode, date).mapScheduleToModel()
  } else {
    scheduleRepository.getScheduleForProbationTeam(probationTeamCode, date).mapScheduleToModel()
  }

  private fun List<ScheduleItemEntity>.mapScheduleToModel(): List<ScheduleItem> {
    val locations = mapNotNull { locationsService.getLocationById(it.prisonLocationId) }
    return toModel(locations)
  }
}

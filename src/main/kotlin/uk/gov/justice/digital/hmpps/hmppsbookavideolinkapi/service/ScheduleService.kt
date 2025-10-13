package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.between
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationStatus
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
    scheduleRepository.getScheduleForPrisonIncludingCancelled(prisonCode, date).mapScheduleToModel(date)
  } else {
    scheduleRepository.getScheduleForPrison(prisonCode, date).mapScheduleToModel(date)
  }

  fun getScheduleForCourt(courtCode: String, date: LocalDate, includeCancelled: Boolean): List<ScheduleItem> = if (includeCancelled) {
    scheduleRepository.getScheduleForCourtIncludingCancelled(courtCode, date).mapScheduleToModel(date)
  } else {
    scheduleRepository.getScheduleForCourt(courtCode, date).mapScheduleToModel(date)
  }

  fun getScheduleForProbationTeam(probationTeamCode: String, date: LocalDate, includeCancelled: Boolean): List<ScheduleItem> = if (includeCancelled) {
    scheduleRepository.getScheduleForProbationTeamIncludingCancelled(probationTeamCode, date).mapScheduleToModel(date)
  } else {
    scheduleRepository.getScheduleForProbationTeam(probationTeamCode, date).mapScheduleToModel(date)
  }

  private fun List<ScheduleItemEntity>.mapScheduleToModel(onDate: LocalDate): List<ScheduleItem> = run {
    toModel(mapNotNull { locationsService.getLocationById(it.prisonLocationId) }) { it.checkAvailability(onDate) }
  }

  private fun Location.checkAvailability(onDate: LocalDate) = run {
    when (this.extraAttributes?.locationStatus) {
      LocationStatus.ACTIVE -> false
      LocationStatus.INACTIVE -> true
      LocationStatus.TEMPORARILY_BLOCKED -> onDate.between(this.extraAttributes.blockedFrom!!, this.extraAttributes.blockedTo!!)
      null -> false
    }
  }
}

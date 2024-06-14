package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.response.ScheduleItem
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import java.time.LocalDate

@Service
class ScheduleService(
  private val scheduleRepository: ScheduleRepository,
) {
  fun getScheduleForPrison(prisonCode: String, date: LocalDate, includeCancelled: Boolean): List<ScheduleItem> {
    return if (includeCancelled) {
      scheduleRepository.getScheduleForPrisonIncludingCancelled(prisonCode, date).toModel()
    } else {
      scheduleRepository.getScheduleForPrison(prisonCode, date).toModel()
    }
  }

  fun getScheduleForCourt(courtCode: String, date: LocalDate, includeCancelled: Boolean): List<ScheduleItem> {
    return if (includeCancelled) {
      scheduleRepository.getScheduleForCourtIncludingCancelled(courtCode, date).toModel()
    } else {
      scheduleRepository.getScheduleForCourt(courtCode, date).toModel()
    }
  }

  fun getScheduleForProbationTeam(probationTeamCode: String, date: LocalDate, includeCancelled: Boolean): List<ScheduleItem> {
    return if (includeCancelled) {
      scheduleRepository.getScheduleForProbationTeamIncludingCancelled(probationTeamCode, date).toModel()
    } else {
      scheduleRepository.getScheduleForProbationTeam(probationTeamCode, date).toModel()
    }
  }
}

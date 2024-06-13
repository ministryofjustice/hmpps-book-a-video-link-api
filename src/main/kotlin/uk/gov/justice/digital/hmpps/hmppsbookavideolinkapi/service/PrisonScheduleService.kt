package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonScheduleRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import java.time.LocalDate

@Service
class PrisonScheduleService(
  private val roomBookingRepository: PrisonScheduleRepository,
) {
  fun getSchedule(date: LocalDate, prisonCode: String) =
    roomBookingRepository.getSchedule(forDate = date, forPrison = prisonCode).toModel()

  fun getScheduleIncludingCancelled(date: LocalDate, prisonCode: String) =
    roomBookingRepository.getScheduleIncludingCancelled(forDate = date, forPrison = prisonCode).toModel()
}

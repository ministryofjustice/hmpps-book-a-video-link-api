package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PagedModel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.between
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.StringFeature
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.FindCourtBookingsRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.request.FindProbationBookingsRequest
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
  private val prisonerSearchClient: PrisonerSearchClient,
  private val featureSwitches: FeatureSwitches,
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

  fun getScheduleForProbationTeamsPaginated(request: FindProbationBookingsRequest, pageable: Pageable): PagedModel<ScheduleItem> {
    val pageOfResults = scheduleRepository.getScheduleForProbationTeamsPaginated(
      probationTeamCodes = request.probationTeamCodes!!.distinct(),
      fromDate = request.fromDate,
      toDate = request.toDate,
      excludingPrisons = excludeCourtOnlyPrisons(),
      pageable,
    )
    val modelContent = pageOfResults.content.mapScheduleToModel()
    return PagedModel(PageImpl(modelContent, pageable, pageOfResults.totalElements))
  }

  fun getScheduleForCourtsPaginated(request: FindCourtBookingsRequest, pageable: Pageable): PagedModel<ScheduleItem> {
    val pageOfResults = scheduleRepository.getScheduleForCourtsPaginated(
      courtCodes = request.courtCodes!!.distinct(),
      fromDate = request.fromDate,
      toDate = request.toDate,
      excludingPrisons = excludeProbationOnlyPrisons(),
      pageable,
    )
    val modelContent = pageOfResults.content.mapScheduleToModel()
    return PagedModel(PageImpl(modelContent, pageable, pageOfResults.totalElements))
  }

  fun getScheduleForProbationTeamsUnpaginated(request: FindProbationBookingsRequest, sort: Sort): List<ScheduleItem> = run {
    scheduleRepository.getScheduleForProbationTeamsUnpaginated(
      probationTeamCodes = request.probationTeamCodes!!.distinct(),
      fromDate = request.fromDate,
      toDate = request.toDate,
      excludingPrisons = excludeCourtOnlyPrisons(),
      sort,
    ).mapScheduleToModel()
  }

  fun getScheduleForCourtsUnpaginated(request: FindCourtBookingsRequest, sort: Sort): List<ScheduleItem> = run {
    scheduleRepository.getScheduleForCourtsUnpaginated(
      courtCodes = request.courtCodes!!.distinct(),
      fromDate = request.fromDate,
      toDate = request.toDate,
      excludingPrisons = excludeProbationOnlyPrisons(),
      sort,
    ).mapScheduleToModel()
  }

  private fun excludeCourtOnlyPrisons() = featureSwitches.getValue(StringFeature.FEATURE_COURT_ONLY_PRISONS, null)?.split(',')

  private fun excludeProbationOnlyPrisons() = featureSwitches.getValue(StringFeature.FEATURE_PROBATION_ONLY_PRISONS, null)?.split(',')

  private fun List<ScheduleItemEntity>.mapScheduleToModel(): List<ScheduleItem> = run {
    toModel(
      prisoners = prisonerSearchClient.findByPrisonerNumbers(map { it.prisonerNumber }.toSet()),
      locations = mapNotNull { locationsService.getLocationById(it.prisonLocationId) },
      availabilityChecker = { location, onDate -> location.checkAvailability(onDate) },
    )
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

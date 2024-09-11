package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.migration

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ProbationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository

@Service
class MigrateMappingService(
  private val prisonerSearchClient: PrisonerSearchClient,
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient,
  private val probationTeamRepository: ProbationTeamRepository,
  private val courtRepository: CourtRepository,
) {
  fun mapBookingIdToPrisonerNumber(bookingId: Long): String? =
    prisonerSearchClient.getPrisoner(bookingId)?.prisonerNumber
  fun mapInternalLocationIdsToLocations(vararg internalLocationId: Long): Map<Long, String> = TODO()
  fun mapCourtCodeToCourt(courtCode: String): Court? =
    courtRepository.findByCode(courtCode)
  fun mapProbationTeamCodeToProbationTeam(probationTeamCode: String): ProbationTeam? =
    probationTeamRepository.findByCode(probationTeamCode)
}

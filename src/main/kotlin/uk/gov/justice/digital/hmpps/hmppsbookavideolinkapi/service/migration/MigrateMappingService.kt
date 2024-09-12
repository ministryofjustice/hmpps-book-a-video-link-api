package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.migration

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.MigrationClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository

@Service
class MigrateMappingService(
  private val prisonerSearchClient: PrisonerSearchClient,
  private val migrationClient: MigrationClient,
  private val probationTeamRepository: ProbationTeamRepository,
  private val courtRepository: CourtRepository,
) {
  fun mapBookingIdToPrisonerNumber(bookingId: Long): String? =
    prisonerSearchClient.getPrisoner(bookingId)?.prisonerNumber

  fun mapInternalLocationIdToLocation(id: Long) = migrationClient.getLocationByInternalId(id)

  fun mapCourtCodeToCourt(code: String) = courtRepository.findByCode(code)
  fun mapProbationTeamCodeToProbationTeam(code: String) = probationTeamRepository.findByCode(code)
}

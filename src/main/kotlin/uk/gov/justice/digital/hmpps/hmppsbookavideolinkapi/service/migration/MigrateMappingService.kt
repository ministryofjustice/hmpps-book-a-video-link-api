package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.migration

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.MigrationClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository

@Service
@Deprecated(message = "Can be removed when migration is completed")
class MigrateMappingService(
  private val migrationClient: MigrationClient,
  private val probationTeamRepository: ProbationTeamRepository,
  private val courtRepository: CourtRepository,
) {
  fun mapBookingIdToPrisonerNumber(bookingId: Long): String? =
    migrationClient.getPrisonerByBookingId(bookingId)?.offenderNo

  fun mapInternalLocationIdToDpsLocationId(id: Long) = migrationClient.getDpsLocationIdByInternalId(id)

  fun mapCourtCodeToCourt(code: String) = courtRepository.findByCode(code)

  fun mapProbationTeamCodeToProbationTeam(code: String) = probationTeamRepository.findByCode(code)
}

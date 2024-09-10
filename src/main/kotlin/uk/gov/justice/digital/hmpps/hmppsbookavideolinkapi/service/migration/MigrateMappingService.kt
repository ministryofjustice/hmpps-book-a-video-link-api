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
  fun mapBookingIdToPrisonerNumber(bookingId: Long): String = TODO()
  fun mapInternalLocationIdToLocationBusinessKey(internalLocationId: Long): String = TODO()
  fun mapCourtCodeToCourt(courtCode: String): Court = TODO()
  fun mapProbationTeamCodeToProbationTeam(probationTeamCode: String): ProbationTeam = TODO()
}

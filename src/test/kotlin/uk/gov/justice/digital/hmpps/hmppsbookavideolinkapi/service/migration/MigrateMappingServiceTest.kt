package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.migration

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.locationsinsideprison.LocationsInsidePrisonClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository

class MigrateMappingServiceTest {
  private val prisonerSearchClient: PrisonerSearchClient = mock()
  private val locationsInsidePrisonClient: LocationsInsidePrisonClient = mock()
  private val probationTeamRepository: ProbationTeamRepository = mock()
  private val courtRepository: CourtRepository = mock()
  private val service = MigrateMappingService(prisonerSearchClient, locationsInsidePrisonClient, probationTeamRepository, courtRepository)

  @Test
  fun `should do something`() {
  }
}

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.migration

import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.migration.MigrationClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.court
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvilleLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationTeam
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.CourtRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository

class MigrateMappingServiceTest {
  private val migrationClient: MigrationClient = mock()
  private val probationTeamRepository: ProbationTeamRepository = mock()
  private val courtRepository: CourtRepository = mock()
  private val service =
    MigrateMappingService(migrationClient, probationTeamRepository, courtRepository)

  @Test
  fun `should map prisoner booking id to prisoner number`() {
    migrationClient.stub {
      on { getPrisonerByBookingId(1) } doReturn MigrationClient.Prisoner("ABC123")
      on { getPrisonerByBookingId(2) } doReturn MigrationClient.Prisoner("DEF456")
      on { getPrisonerByBookingId(3) } doReturn MigrationClient.Prisoner("GHI789")
    }

    service.mapBookingIdToPrisonerNumber(1) isEqualTo "ABC123"
    service.mapBookingIdToPrisonerNumber(2) isEqualTo "DEF456"
    service.mapBookingIdToPrisonerNumber(3) isEqualTo "GHI789"
  }

  @Test
  fun `should map court code to court`() {
    courtRepository.stub {
      on { findByCode("ABC") } doReturn court(code = "ABC")
      on { findByCode("DEF") } doReturn court(code = "DEF")
      on { findByCode("GHI") } doReturn court(code = "GHI")
    }

    service.mapCourtCodeToCourt("ABC") isEqualTo court(code = "ABC")
    service.mapCourtCodeToCourt("DEF") isEqualTo court(code = "DEF")
    service.mapCourtCodeToCourt("GHI") isEqualTo court(code = "GHI")
  }

  @Test
  fun `should map probation team code to probation team`() {
    probationTeamRepository.stub {
      on { findByCode("ABC") } doReturn probationTeam(code = "ABC")
      on { findByCode("DEF") } doReturn probationTeam(code = "DEF")
      on { findByCode("GHI") } doReturn probationTeam(code = "GHI")
    }

    service.mapProbationTeamCodeToProbationTeam("ABC") isEqualTo probationTeam(code = "ABC")
    service.mapProbationTeamCodeToProbationTeam("DEF") isEqualTo probationTeam(code = "DEF")
    service.mapProbationTeamCodeToProbationTeam("GHI") isEqualTo probationTeam(code = "GHI")
  }

  @Test
  fun `should map internal location id to DPS location`() {
    migrationClient.stub {
      on { getLocationByInternalId(1) } doReturn wandsworthLocation
      on { getLocationByInternalId(2) } doReturn birminghamLocation
      on { getLocationByInternalId(3) } doReturn pentonvilleLocation
    }

    service.mapInternalLocationIdToLocation(1) isEqualTo wandsworthLocation
    service.mapInternalLocationIdToLocation(2) isEqualTo birminghamLocation
    service.mapInternalLocationIdToLocation(3) isEqualTo pentonvilleLocation
  }
}

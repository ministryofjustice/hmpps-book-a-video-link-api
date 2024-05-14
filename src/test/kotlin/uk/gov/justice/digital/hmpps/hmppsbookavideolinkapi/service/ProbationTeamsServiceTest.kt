package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ProbationTeam as ProbationTeamEntity

class ProbationTeamsServiceTest {
  private val probationTeamRepository: ProbationTeamRepository = mock()
  private val service = ProbationTeamsService(probationTeamRepository)
  private fun generateEntity(id: Long, code: String, desc: String, enabled: Boolean = true, notes: String? = "notes") =
    ProbationTeamEntity(id, code, desc, enabled, notes, "name")

  @BeforeEach
  fun setUp() {
    openMocks(this)
  }

  @Test
  fun `Should return a list of enabled probation teams`() {
    val listOfEnabledTeams = listOf(
      generateEntity(1L, "NORTH", "North"),
      generateEntity(2L, "SOUTH", "South"),
      generateEntity(3L, "EAST", "East"),
      generateEntity(4L, "WEST", "West"),
    )

    whenever(probationTeamRepository.findAllByEnabledIsTrue()).thenReturn(listOfEnabledTeams)

    assertThat(service.getEnabledProbationTeams()).isEqualTo(
      listOfEnabledTeams.toModel(),
    )

    verify(probationTeamRepository).findAllByEnabledIsTrue()
  }

  @Test
  fun `Should return an empty list when no probation teams are enabled`() {
    whenever(probationTeamRepository.findAllByEnabledIsTrue()).thenReturn(emptyList())
    assertThat(service.getEnabledProbationTeams()).isEmpty()
    verify(probationTeamRepository).findAllByEnabledIsTrue()
  }
}

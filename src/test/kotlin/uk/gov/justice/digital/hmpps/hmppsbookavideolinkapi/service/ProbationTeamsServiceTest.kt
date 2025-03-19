package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations.openMocks
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactly
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ProbationTeamRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.UserProbationRepository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.mapping.toModel
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ProbationTeam as ProbationTeamEntity

class ProbationTeamsServiceTest {
  private val probationTeamRepository: ProbationTeamRepository = mock()
  private val userProbationRepository: UserProbationRepository = mock()
  private val service = ProbationTeamsService(probationTeamRepository, userProbationRepository)
  private fun generateEntity(id: Long, code: String, desc: String, enabled: Boolean = true, notes: String? = "notes") = ProbationTeamEntity(id, code, desc, enabled, false, notes, "name")

  @BeforeEach
  fun setUp() {
    openMocks(this)
  }

  @Test
  fun `Should get user-probation team preferences for a user`() {
    val listOfTeamsForUser = listOf(
      generateEntity(1L, "NORTH", "One"),
      generateEntity(2L, "SOUTH", "Two"),
    )

    whenever(probationTeamRepository.findProbationTeamsByUsername("user")).thenReturn(listOfTeamsForUser)

    assertThat(service.getUserProbationTeamPreferences(probationUser("user"))).isEqualTo(
      listOfTeamsForUser.toModel(),
    )

    verify(probationTeamRepository).findProbationTeamsByUsername("user")
  }

  @Test
  fun `Should return enabled teams only sorted`() {
    val enabledTeams = listOf(
      generateEntity(1L, "NORTH", "North"),
      generateEntity(2L, "SOUTH", "South"),
      generateEntity(3L, "EAST", "East"),
      generateEntity(4L, "WEST", "West"),
    )

    val disabledTeams = listOf(
      generateEntity(5L, "NORTHWEST", "North West", enabled = false),
    )

    val allTeams = enabledTeams.plus(disabledTeams)

    whenever(probationTeamRepository.findAllByEnabledIsTrue()) doReturn (enabledTeams)
    whenever(probationTeamRepository.findAll()) doReturn allTeams

    service.getProbationTeams(true) containsExactlyInAnyOrder enabledTeams.toModel()
    verify(probationTeamRepository, never()).findAll()
  }

  @Test
  fun `Should return all teams sorted with enabled having priority order`() {
    val enabledTeams = listOf(
      generateEntity(1L, "NORTH", "North"),
      generateEntity(2L, "SOUTH", "South"),
      generateEntity(3L, "EAST", "East"),
      generateEntity(4L, "WEST", "West"),
    )

    val disabledTeams = listOf(
      generateEntity(5L, "NORTHWEST", "North West", enabled = false),
      generateEntity(6L, "NORTHEAST", "North East", enabled = false),
    )

    val allTeams = enabledTeams.plus(disabledTeams)

    whenever(probationTeamRepository.findAllByEnabledIsTrue()) doReturn (enabledTeams)
    whenever(probationTeamRepository.findAll()) doReturn allTeams

    service.getProbationTeams(false) containsExactly listOf(
      generateEntity(3L, "EAST", "East").toModel(),
      generateEntity(1L, "NORTH", "North").toModel(),
      generateEntity(2L, "SOUTH", "South").toModel(),
      generateEntity(4L, "WEST", "West").toModel(),
      generateEntity(6L, "NORTHEAST", "North East", enabled = false).toModel(),
      generateEntity(5L, "NORTHWEST", "North West", enabled = false).toModel(),
    )
    verify(probationTeamRepository, never()).findAllByEnabledIsTrue()
  }
}

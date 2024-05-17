package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.ProbationTeam

@Repository
interface ProbationTeamRepository : JpaRepository<ProbationTeam, Long> {
  fun findAllByEnabledIsTrue(): List<ProbationTeam>

  @Query(
    value = """
      SELECT pt.* 
      FROM probation_team pt
      JOIN user_probation up ON up.probation_team_id = pt.probation_team_id AND up.username = :username
      WHERE pt.enabled = true
      """,
    nativeQuery = true,
  )
  fun findProbationTeamsByUsername(username: String): List<ProbationTeam>
  fun findAllByCodeIn(codes: List<String>): List<ProbationTeam>
}

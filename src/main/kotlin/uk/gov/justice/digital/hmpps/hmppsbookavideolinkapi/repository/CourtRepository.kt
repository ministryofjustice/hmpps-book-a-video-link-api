package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Court

@Repository
interface CourtRepository : JpaRepository<Court, Long> {
  fun findAllByEnabledIsTrue(): List<Court>

  @Query(
    value = """
      SELECT c.*  
      FROM court c
      JOIN user_court uc ON uc.court_id = c.court_id AND uc.username = :username
      WHERE c.enabled = true
      """,
    nativeQuery = true,
  )
  fun findCourtsByUsername(username: String): List<Court>

  fun findAllByCodeIn(codes: List<String>): List<Court>
}

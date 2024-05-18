package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.UserProbation

@Repository
interface UserProbationRepository : JpaRepository<UserProbation, Long> {
  @Query(value = "from UserProbation up where up.username = :username")
  fun findAllByUsername(@Param("username") username: String): List<UserProbation>
}

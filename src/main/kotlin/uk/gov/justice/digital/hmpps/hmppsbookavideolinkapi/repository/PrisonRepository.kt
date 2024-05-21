package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.Prison

@Repository
interface PrisonRepository : JpaRepository<Prison, Long> {
  fun findByCode(prisonCode: String): Prison?
}

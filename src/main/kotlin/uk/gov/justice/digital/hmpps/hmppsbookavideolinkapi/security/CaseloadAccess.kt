package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User

/**
 * Caseload access checks are only relevant for prison users.
 * If the current user is not a prison user then the check will be silently ignored.
 */
fun checkCaseLoadAccess(user: User, prisonCode: String) {
  if (user is PrisonUser && user.activeCaseLoadId != prisonCode) {
    throw CaseloadAccessException()
  }
}

class CaseloadAccessException : RuntimeException()

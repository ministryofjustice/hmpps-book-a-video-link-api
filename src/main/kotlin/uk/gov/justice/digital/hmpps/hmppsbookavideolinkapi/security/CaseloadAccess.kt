package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security

import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.PrisonUser
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserType

/**
 * Caseload access checks are only relevant for prison users. If the current user is not a prison user then it will just
 * be ignored.
 */
fun checkCaseLoadAccess(prisonUser: User, prisonCode: String) {
  if (prisonUser.isUserType(UserType.PRISON) && (prisonUser as PrisonUser).activeCaseLoadId != prisonCode) throw CaseloadAccessException()
}

class CaseloadAccessException : RuntimeException()

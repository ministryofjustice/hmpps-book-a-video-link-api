package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security

import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.getBvlsRequestContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.UserType

/**
 * Caseload access checks are only relevant for prison users. If the current user is not a prison user then it will just
 * be ignored.
 */
fun checkCaseLoadAccess(prisonCode: String) {
  val httpRequest =
    if (RequestContextHolder.getRequestAttributes() != null) {
      (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request
    } else {
      throw NullPointerException("Unable to determine user from request context.")
    }

  httpRequest.getBvlsRequestContext().user.takeIf { it.isUserType(UserType.PRISON) }?.run {
    if (prisonCode != activeCaseLoadId) {
      throw CaseloadAccessException()
    }
  }
}

class CaseloadAccessException : RuntimeException()

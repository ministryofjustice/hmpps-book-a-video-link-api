package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security

import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.BvlsRequestContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.User

fun addUserToRequestForCaseloadCheck(user: User) {
  val mockRequest = MockHttpServletRequest()
  mockRequest.setAttribute(BvlsRequestContext::class.simpleName!!, BvlsRequestContext(user))
  RequestContextHolder.setRequestAttributes(ServletRequestAttributes(mockRequest))
}

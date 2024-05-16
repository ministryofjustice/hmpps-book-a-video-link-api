package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isCloseTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken
import java.time.LocalDateTime

class BvlsRequestContextConfigurationTest {

  private val interceptor = BvlsRequestContextInterceptor()
  private val req = MockHttpServletRequest()
  private val res = MockHttpServletResponse()

  @Test
  fun `should populate request context user_name for TEST_USER`() {
    setSecurityContext(mapOf("user_name" to "USER_NAME"))

    interceptor.preHandle(req, res, "null")

    val context = req.getAttribute(BvlsRequestContext::class.simpleName!!) as BvlsRequestContext

    context.username isEqualTo "USER_NAME"
    context.requestAt isCloseTo LocalDateTime.now()
  }

  @Test
  fun `should populate request context username for TEST_USER`() {
    setSecurityContext(mapOf("username" to "USERNAME"))

    interceptor.preHandle(req, res, "null")

    val context = req.getAttribute(BvlsRequestContext::class.simpleName!!) as BvlsRequestContext

    context.username isEqualTo "USERNAME"
    context.requestAt isCloseTo LocalDateTime.now()
  }

  @Test
  fun `should populate request context client_id for TEST_USER`() {
    setSecurityContext(mapOf("client_id" to "CLIENT_ID"))

    interceptor.preHandle(req, res, "null")

    val context = req.getAttribute(BvlsRequestContext::class.simpleName!!) as BvlsRequestContext

    context.username isEqualTo "CLIENT_ID"
    context.requestAt isCloseTo LocalDateTime.now()
  }

  @Test
  fun `should throw AccessDeniedException when authentication is null`() {
    SecurityContextHolder.setContext(mock { on { authentication } doReturn null })

    val exception = assertThrows<AccessDeniedException> { interceptor.preHandle(req, res, "null") }

    exception.message isEqualTo "User is not authenticated"
  }

  @AfterEach
  fun afterEach() {
    SecurityContextHolder.clearContext()
  }

  private fun setSecurityContext(claims: Map<String, Any>) =
    mock<AuthAwareAuthenticationToken> { on { tokenAttributes } doReturn claims }.also {
        token ->
      SecurityContextHolder.setContext(mock { on { authentication } doReturn token })
    }
}

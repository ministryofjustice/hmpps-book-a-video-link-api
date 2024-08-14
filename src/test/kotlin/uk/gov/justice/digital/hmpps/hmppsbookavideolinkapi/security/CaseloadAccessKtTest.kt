package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.web.context.request.RequestContextHolder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.EXTERNAL_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isNotEqualTo

class CaseloadAccessKtTest {

  @BeforeEach
  fun before() {
    RequestContextHolder.resetRequestAttributes()
  }

  @Test
  fun `should fail caseload check when missing request attributes`() {
    RequestContextHolder.getRequestAttributes() isEqualTo null

    val error = assertThrows<NullPointerException> { checkCaseLoadAccess("DONT_CARE") }

    error.message isEqualTo "Unable to determine user from request context."
  }

  @Test
  fun `should fail caseload check when does not match that of prison user`() {
    addUserToRequestForCaseloadCheck(PRISON_USER.copy(activeCaseLoadId = "THIS"))

    RequestContextHolder.getRequestAttributes() isNotEqualTo null

    assertThrows<CaseloadAccessException> { checkCaseLoadAccess("THAT") }
  }

  @Test
  fun `should succeed caseload check when does match that of prison user`() {
    addUserToRequestForCaseloadCheck(PRISON_USER.copy(activeCaseLoadId = "MATCH"))

    RequestContextHolder.getRequestAttributes() isNotEqualTo null

    assertDoesNotThrow { checkCaseLoadAccess("MATCH") }
  }

  @Test
  fun `should succeed caseload check when user is external user`() {
    addUserToRequestForCaseloadCheck(EXTERNAL_USER)

    RequestContextHolder.getRequestAttributes() isNotEqualTo null

    assertDoesNotThrow { checkCaseLoadAccess("DONT_CARE") }
  }
}

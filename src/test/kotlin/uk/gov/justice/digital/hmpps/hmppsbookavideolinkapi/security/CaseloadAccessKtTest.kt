package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.security

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.EXTERNAL_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.SERVICE_USER

class CaseloadAccessKtTest {
  @Test
  fun `should fail caseload check when does not match that of prison user`() {
    assertThrows<CaseloadAccessException> { checkCaseLoadAccess(PRISON_USER.copy(activeCaseLoadId = "THIS"), "THAT") }
  }

  @Test
  fun `should succeed caseload check when does match that of prison user`() {
    assertDoesNotThrow { checkCaseLoadAccess(PRISON_USER.copy(activeCaseLoadId = "MATCH"), "MATCH") }
  }

  @Test
  fun `should succeed caseload check when user is not a prison user`() {
    assertDoesNotThrow { checkCaseLoadAccess(EXTERNAL_USER, "DONT_CARE") }
    assertDoesNotThrow { checkCaseLoadAccess(SERVICE_USER, "DONT_CARE") }
  }
}

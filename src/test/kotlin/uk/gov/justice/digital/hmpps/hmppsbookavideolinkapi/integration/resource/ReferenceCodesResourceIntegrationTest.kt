package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ReferenceCodeRepository

class ReferenceCodesResourceIntegrationTest : IntegrationTestBase() {
  @Autowired
  private lateinit var referenceCodeRepository: ReferenceCodeRepository

  @Test
  fun `should return a list of court hearing type reference codes`() {
    val groupCode = "COURT_HEARING_TYPE"
    referenceCodeRepository.findAllByGroupCodeEquals(groupCode) hasSize 25

    val listOfCodes = webTestClient.getReferenceCodes(groupCode)

    assertThat(listOfCodes).hasSize(25)
    assertThat(listOfCodes)
      .extracting("code")
      .containsAll(listOf("TRIBUNAL", "APPEAL", "BACKER", "TRIAL", "POCA", "REMAND"))
    assertThat(listOfCodes).extracting("code").doesNotContainAnyElementsOf(listOf("RR", "PSR"))
  }

  @Test
  fun `should return a list of probation meeting type reference codes`() {
    val groupCode = "PROBATION_MEETING_TYPE"
    referenceCodeRepository.findAllByGroupCodeEquals(groupCode) hasSize 2

    val listOfCodes = webTestClient.getReferenceCodes(groupCode)

    assertThat(listOfCodes).extracting("code").containsExactlyInAnyOrder("PSR", "RR")
  }

  @Test
  fun `should return a list of appointment type values`() {
    val groupCode = "APPOINTMENT_TYPE"
    referenceCodeRepository.findAllByGroupCodeEquals(groupCode) hasSize 4

    val listOfCodes = webTestClient.getReferenceCodes(groupCode)

    assertThat(listOfCodes)
      .extracting("code")
      .containsExactlyInAnyOrder("VLB_COURT_PRE", "VLB_COURT_MAIN", "VLB_COURT_POST", "VLB_PROBATION")
  }

  @Test
  fun `should return an empty list if the group code does not match any values`() {
    val groupCode = "INCORRECT_TYPE"
    referenceCodeRepository.findAllByGroupCodeEquals(groupCode) hasSize 0

    val listOfCodes = webTestClient.getReferenceCodes(groupCode)

    assertThat(listOfCodes).hasSize(0)
  }

  private fun WebTestClient.getReferenceCodes(groupCode: String?) =
    get()
      .uri("/reference-codes/group/{groupCode}", groupCode)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation("user", roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(ReferenceCode::class.java)
      .returnResult().responseBody
}

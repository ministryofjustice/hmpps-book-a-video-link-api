package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.ReferenceCode
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.ReferenceCodeRepository

class ReferenceCodesResourceIntegrationTest : IntegrationTestBase() {
  @Autowired
  private lateinit var referenceCodeRepository: ReferenceCodeRepository

  @Test
  fun `should return a list of court hearing type reference codes`() {
    val groupCode = "COURT_HEARING_TYPE"
    referenceCodeRepository.findAllByGroupCodeEquals(groupCode) hasSize 26

    val enabledCourtHearingTypes = webTestClient.getReferenceCodes(groupCode)

    assertThat(enabledCourtHearingTypes).hasSize(26)
    assertThat(enabledCourtHearingTypes)
      .extracting("code")
      .containsAll(listOf("TRIBUNAL", "APPEAL", "BACKER", "TRIAL", "POCA", "REMAND", "UNKNOWN"))
    assertThat(enabledCourtHearingTypes).extracting("code").doesNotContainAnyElementsOf(listOf("RR", "PSR"))

    val allCourtHearingTypes = webTestClient.getReferenceCodes(groupCode, false)

    // The two should be the same as there are no disabled courts, UNKNOWN is enabled for courts
    allCourtHearingTypes isEqualTo enabledCourtHearingTypes
    allCourtHearingTypes.all { it.enabled } isBool true
  }

  @Test
  fun `should return a list of ordered probation meeting type reference codes`() {
    val groupCode = "PROBATION_MEETING_TYPE"
    referenceCodeRepository.findAllByGroupCodeEquals(groupCode) hasSize 15

    val enabledOnlyProbationMeetingTypes = webTestClient.getReferenceCodes(groupCode)

    assertThat(enabledOnlyProbationMeetingTypes).extracting("code").containsExactly(
      "PSR",
      "FTR56",
      "RR",
      "PR",
      "HDC",
      "OASYS",
      "MALRAP",
      "PRP",
      "IOM",
      "RTSCR",
      "BR",
      "RCAT",
      "ROTL",
      "OTHER",
      "UNKNOWN",
    )
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

  private fun WebTestClient.getReferenceCodes(groupCode: String, enabledOnly: Boolean = true) = get()
    .uri("/reference-codes/group/{groupCode}?enabledOnly=$enabledOnly", groupCode)
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(ReferenceCode::class.java)
    .returnResult().responseBody!!
}

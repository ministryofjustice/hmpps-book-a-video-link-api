package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.MOORLAND
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository

class PrisonsResourceIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var prisonRepository: PrisonRepository

  @Test
  fun `should return a list of enabled prisons`() {
    prisonRepository.findAllByEnabledIsTrue() hasSize 14

    val listOfEnabledPrisons = webTestClient.getPrisons(true)

    assertThat(listOfEnabledPrisons).hasSize(14)
    assertThat(listOfEnabledPrisons).extracting("code").contains("WWI")
    assertThat(listOfEnabledPrisons).extracting("code").doesNotContain("LEI")
  }

  @Test
  fun `should return a list of all prisons`() {
    prisonRepository.findAll() hasSize 112

    val listOfAllPrisons = webTestClient.getPrisons(false)

    assertThat(listOfAllPrisons).hasSize(112)
    assertThat(listOfAllPrisons).extracting("code").containsAll(listOf("LEI", "BMI", "WWI"))
  }

  private fun WebTestClient.getPrisons(enabledOnly: Boolean = false) =
    get()
      .uri {
        it.path("/prisons/list")
          .queryParam("enabledOnly", enabledOnly)
          .build()
      }
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation("user", roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(Prison::class.java)
      .returnResult().responseBody

  @Test
  fun `should get video link locations only`() {
    locationsInsidePrisonApi().stubVideoLinkLocationsAtPrison(setOf("VIDEOLINK"), prisonCode = MOORLAND)
    locationsInsidePrisonApi().stubNonResidentialAppointmentLocationsAtPrison(setOf("NOT_VIDEO_LINK"), prisonCode = MOORLAND)

    val response = webTestClient.getAppointmentLocations(MOORLAND)

    response.single() isEqualTo Location(key = "VIDEOLINK", description = "$MOORLAND VIDEOLINK", true)
  }

  @Test
  fun `should get all possible appointment locations`() {
    locationsInsidePrisonApi().stubNonResidentialAppointmentLocationsAtPrison(setOf("VIDEOLINK", "NOT_VIDEO_LINK"), prisonCode = MOORLAND)

    val response = webTestClient.getAppointmentLocations(MOORLAND, "?videoLinkOnly=false")

    response containsExactlyInAnyOrder listOf(
      Location(key = "VIDEOLINK", description = "$MOORLAND VIDEOLINK", true),
      Location(key = "NOT_VIDEO_LINK", description = "$MOORLAND NOT_VIDEO_LINK", true),
    )
  }

  @Test
  fun `should be no appointment locations if not enabled`() {
    locationsInsidePrisonApi().stubNonResidentialAppointmentLocationsAtPrison(setOf("VIDEOLINK", "NOT_VIDEO_LINK"), enabled = false, prisonCode = MOORLAND)

    val response = webTestClient.getAppointmentLocations(MOORLAND, "?videoLinkOnly=false&enabledOnly=true")

    response.isEmpty() isBool true
  }

  @Test
  fun `should be no video link locations if not enabled`() {
    locationsInsidePrisonApi().stubVideoLinkLocationsAtPrison(setOf("VIDEOLINK"), enabled = false, prisonCode = MOORLAND)

    val response = webTestClient.getAppointmentLocations(MOORLAND, "?enabledOnly=true")

    response.isEmpty() isBool true
  }

  private fun WebTestClient.getAppointmentLocations(prisonCode: String, requestParams: String = "") =
    get()
      .uri("/prisons/$prisonCode/locations$requestParams")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(Location::class.java)
      .returnResult().responseBody!!
}

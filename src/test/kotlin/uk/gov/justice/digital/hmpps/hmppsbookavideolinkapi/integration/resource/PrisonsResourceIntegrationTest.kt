package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
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
  private lateinit var cacheManager: CacheManager

  @Autowired
  private lateinit var prisonRepository: PrisonRepository

  @BeforeEach
  fun before() {
    // Some location endpoints are being cached, so we need to clear them so as not to impact the unit tests in this class.
    cacheManager.clearAllCaches()
  }

  @Test
  fun `should return a list of enabled prisons`() {
    prisonRepository.findAllByEnabledIsTrue() hasSize 15

    val listOfEnabledPrisons = webTestClient.getPrisons(true)

    assertThat(listOfEnabledPrisons).hasSize(15)
    assertThat(listOfEnabledPrisons).extracting("code").contains("WWI")
    assertThat(listOfEnabledPrisons).extracting("code").doesNotContain("LEI")
  }

  @Test
  fun `should return a list of all prisons`() {
    prisonRepository.findAll() hasSize 125

    val listOfAllPrisons = webTestClient.getPrisons(false)

    assertThat(listOfAllPrisons).hasSize(125)
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
      .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBodyList(Prison::class.java)
      .returnResult().responseBody

  @Test
  fun `should get video link locations only`() {
    locationsInsidePrisonApi().stubVideoLinkLocationsAtPrison(setOf("VIDEOLINK"), prisonCode = WANDSWORTH)
    locationsInsidePrisonApi().stubNonResidentialAppointmentLocationsAtPrison(setOf("NOT_VIDEO_LINK"), prisonCode = WANDSWORTH)

    val response = webTestClient.getAppointmentLocations(WANDSWORTH)

    response.single() isEqualTo Location(key = "VIDEOLINK", description = "Wwi Videolink", true)
  }

  @Test
  fun `should get all possible appointment locations`() {
    locationsInsidePrisonApi().stubNonResidentialAppointmentLocationsAtPrison(setOf("VIDEOLINK", "NOT_VIDEO_LINK"), prisonCode = WANDSWORTH)

    val response = webTestClient.getAppointmentLocations(WANDSWORTH, "?videoLinkOnly=false")

    response containsExactlyInAnyOrder listOf(
      Location(key = "VIDEOLINK", description = "Wwi Videolink", true),
      Location(key = "NOT_VIDEO_LINK", description = "Wwi Not_video_link", true),
    )
  }

  @Test
  fun `should be no appointment locations if not enabled`() {
    locationsInsidePrisonApi().stubNonResidentialAppointmentLocationsAtPrison(setOf("VIDEOLINK", "NOT_VIDEO_LINK"), enabled = false, prisonCode = WANDSWORTH)

    val response = webTestClient.getAppointmentLocations(WANDSWORTH, "?videoLinkOnly=false&enabledOnly=true")

    response.isEmpty() isBool true
  }

  @Test
  fun `should be no video link locations if not enabled`() {
    locationsInsidePrisonApi().stubVideoLinkLocationsAtPrison(setOf("VIDEOLINK"), enabled = false, prisonCode = WANDSWORTH)

    val response = webTestClient.getAppointmentLocations(WANDSWORTH, "?enabledOnly=true")

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

  private fun CacheManager.clearAllCaches() {
    cacheNames.forEach { cacheManager.getCache(it)?.clear() }
  }
}

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.LocationUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation2
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import java.time.DayOfWeek
import java.time.LocalTime

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

    response.single() isEqualTo Location(key = "VIDEOLINK", description = "WWI VIDEOLINK", true)
  }

  @Test
  fun `should get all possible appointment locations`() {
    locationsInsidePrisonApi().stubNonResidentialAppointmentLocationsAtPrison(
      WANDSWORTH,
      wandsworthLocation.copy(key = "VIDEOLINK", localName = "VCC - ROOM - 1"),
      wandsworthLocation2.copy(key = "NOT_VIDEO_LINK", localName = "PCVL - ROOM - 2"),
    )

    val response = webTestClient.getAppointmentLocations(WANDSWORTH, "?videoLinkOnly=false")

    response containsExactlyInAnyOrder listOf(
      Location(key = "VIDEOLINK", description = "VCC - ROOM - 1", true),
      Location(key = "NOT_VIDEO_LINK", description = "PCVL - ROOM - 2", true),
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

  @Test
  @Sql("classpath:integration-test-data/seed-locations-with-extended-attributes.sql")
  fun `should get video link locations with additional room attributes`() {
    locationsInsidePrisonApi().stubVideoLinkLocationsAtPrison(setOf("VIDEOLINK"), prisonCode = WANDSWORTH)
    locationsInsidePrisonApi().stubNonResidentialAppointmentLocationsAtPrison(setOf("NOT_VIDEO_LINK"), prisonCode = WANDSWORTH)

    val response = webTestClient.getAppointmentLocations(prisonCode = WANDSWORTH, requestParams = "?extendedAttributes=true")

    assertThat(response).isNotEmpty

    with(response[0]) {
      assertThat(key).isEqualTo("VIDEOLINK")
      assertThat(description).isEqualTo("WWI VIDEOLINK")
      assertThat(enabled).isTrue()
      assertThat(extraAttributes).isNotNull

      with(extraAttributes!!) {
        assertThat(locationStatus).isEqualTo(LocationStatus.ACTIVE)
        assertThat(locationUsage).isEqualTo(LocationUsage.SCHEDULE)
        assertThat(prisonVideoUrl).isEqualTo("/video-link/xxx")
        assertThat(notes).isEqualTo("some notes")
        assertThat(schedule).hasSize(3)
        with(schedule[0]) {
          assertThat(startDayOfWeek).isEqualTo(DayOfWeek.MONDAY)
          assertThat(endDayOfWeek).isEqualTo(DayOfWeek.THURSDAY)
          assertThat(startTime).isEqualTo(LocalTime.of(0, 1))
          assertThat(endTime).isEqualTo(LocalTime.of(23, 59))
          assertThat(locationUsage).isEqualTo(LocationUsage.COURT)
        }
        with(schedule[1]) {
          assertThat(startDayOfWeek).isEqualTo(DayOfWeek.FRIDAY)
          assertThat(endDayOfWeek).isEqualTo(DayOfWeek.FRIDAY)
          assertThat(startTime).isEqualTo(LocalTime.of(0, 1))
          assertThat(endTime).isEqualTo(LocalTime.of(23, 59))
          assertThat(locationUsage).isEqualTo(LocationUsage.PROBATION)
        }
        with(schedule[2]) {
          assertThat(startDayOfWeek).isEqualTo(DayOfWeek.SATURDAY)
          assertThat(endDayOfWeek).isEqualTo(DayOfWeek.SUNDAY)
          assertThat(startTime).isEqualTo(LocalTime.of(0, 1))
          assertThat(endTime).isEqualTo(LocalTime.of(23, 59))
          assertThat(locationUsage).isEqualTo(LocationUsage.SHARED)
        }
      }
    }
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

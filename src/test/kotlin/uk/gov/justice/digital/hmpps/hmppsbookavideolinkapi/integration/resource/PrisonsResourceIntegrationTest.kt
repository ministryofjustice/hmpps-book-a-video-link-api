package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.LocationKeyValue
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.wandsworthLocation2
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationScheduleUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationStatus
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.LocationUsage
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.model.Prison
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonRepository
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.UUID

@Sql("classpath:integration-test-data/clean-test-prison.sql")
class PrisonsResourceIntegrationTest : IntegrationTestBase() {

  @Autowired
  private lateinit var prisonRepository: PrisonRepository

  @Test
  fun `should return a list of enabled prisons`() {
    prisonRepository.findAllByEnabledIsTrue() hasSize 18

    val listOfEnabledPrisons = webTestClient.getPrisons(true)

    assertThat(listOfEnabledPrisons).hasSize(18)
    assertThat(listOfEnabledPrisons).extracting("code").contains("WWI")
    assertThat(listOfEnabledPrisons).extracting("code").contains("LPI")
    assertThat(listOfEnabledPrisons).extracting("code").contains("BXI")
    assertThat(listOfEnabledPrisons).extracting("code").contains("CDI")
    assertThat(listOfEnabledPrisons).extracting("code").doesNotContain("LEI")
  }

  @Test
  fun `should return a list of all prisons`() {
    prisonRepository.findAll() hasSize 125

    val listOfAllPrisons = webTestClient.getPrisons(false)

    assertThat(listOfAllPrisons).hasSize(125)
    assertThat(listOfAllPrisons).extracting("code").containsAll(listOf("LEI", "BMI", "WWI"))
  }

  private fun WebTestClient.getPrisons(enabledOnly: Boolean = false) = get()
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
    locationsInsidePrisonApi().stubVideoLinkLocationsAtPrison(WANDSWORTH, wandsworthLocation)
    locationsInsidePrisonApi().stubNonResidentialAppointmentLocationsAtPrison(WANDSWORTH, wandsworthLocation2)

    val response = webTestClient.getAppointmentLocations(WANDSWORTH)

    response.single() isEqualTo Location(key = wandsworthLocation.key, description = wandsworthLocation.localName, enabled = true, dpsLocationId = wandsworthLocation.id, prisonCode = WANDSWORTH)
  }

  @Test
  fun `should return a prisons by its code`() {
    with(webTestClient.getPrison("RSI")!!) { name isEqualTo "Risley (HMP)" }
  }

  private fun WebTestClient.getPrison(prisonCode: String) = get()
    .uri("/prisons/$prisonCode")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody(Prison::class.java)
    .returnResult().responseBody

  @Test
  fun `should get all possible appointment locations`() {
    locationsInsidePrisonApi().stubNonResidentialAppointmentLocationsAtPrison(
      WANDSWORTH,
      wandsworthLocation.copy(key = "VIDEOLINK", localName = "VCC - ROOM - 1"),
      wandsworthLocation2.copy(key = "NOT_VIDEO_LINK", localName = "PCVL - ROOM - 2"),
    )

    val response = webTestClient.getAppointmentLocations(WANDSWORTH, "?videoLinkOnly=false")

    assertThat(response)
      .extracting("key", "description", "enabled")
      .containsAll(
        listOf(
          Tuple("VIDEOLINK", "VCC - ROOM - 1", true),
          Tuple("NOT_VIDEO_LINK", "PCVL - ROOM - 2", true),
        ),
      )
  }

  @Test
  fun `should be no appointment locations if not enabled`() {
    locationsInsidePrisonApi().stubNonResidentialAppointmentLocationsAtPrison(
      listOf(
        LocationKeyValue("VIDEOLINK", UUID.randomUUID()),
        LocationKeyValue("NOT_VIDEO_LINK", UUID.randomUUID()),
      ),
      enabled = false,
      prisonCode = WANDSWORTH,
    )

    val response = webTestClient.getAppointmentLocations(WANDSWORTH, "?videoLinkOnly=false&enabledOnly=true")

    response.isEmpty() isBool true
  }

  @Test
  fun `should be no video link locations if not enabled`() {
    locationsInsidePrisonApi().stubVideoLinkLocationsAtPrison(WANDSWORTH, wandsworthLocation.copy(active = false))

    val response = webTestClient.getAppointmentLocations(WANDSWORTH, "?enabledOnly=true")

    response.isEmpty() isBool true
  }

  // TODO: Not sure why there is a need to run clean-all-data.sql here, as it should be run by the IntegrationTestBase() class
  @Test
  @Sql("classpath:test_data/clean-all-data.sql", "classpath:integration-test-data/seed-locations-with-extended-attributes.sql")
  fun `should get video link locations with additional room attributes`() {
    val seededVideoUUID = UUID.fromString("e58ed763-928c-4155-bee9-fdbaaadc15f3")

    locationsInsidePrisonApi().stubVideoLinkLocationsAtPrison(WANDSWORTH, wandsworthLocation.copy(id = seededVideoUUID))

    val response = webTestClient.getAppointmentLocations(prisonCode = WANDSWORTH)

    assertThat(response).isNotEmpty

    with(response[0]) {
      assertThat(key).isEqualTo(wandsworthLocation.key)
      assertThat(dpsLocationId).isEqualTo(seededVideoUUID)
      assertThat(description).isEqualTo(wandsworthLocation.localName)
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
          assertThat(locationUsage).isEqualTo(LocationScheduleUsage.COURT)
        }
        with(schedule[1]) {
          assertThat(startDayOfWeek).isEqualTo(DayOfWeek.FRIDAY)
          assertThat(endDayOfWeek).isEqualTo(DayOfWeek.FRIDAY)
          assertThat(startTime).isEqualTo(LocalTime.of(0, 1))
          assertThat(endTime).isEqualTo(LocalTime.of(23, 59))
          assertThat(locationUsage).isEqualTo(LocationScheduleUsage.PROBATION)
        }
        with(schedule[2]) {
          assertThat(startDayOfWeek).isEqualTo(DayOfWeek.SATURDAY)
          assertThat(endDayOfWeek).isEqualTo(DayOfWeek.SUNDAY)
          assertThat(startTime).isEqualTo(LocalTime.of(0, 1))
          assertThat(endTime).isEqualTo(LocalTime.of(23, 59))
          assertThat(locationUsage).isEqualTo(LocationScheduleUsage.BLOCKED)
        }
      }
    }
  }

  @Test
  @Sql("classpath:test_data/clean-all-data.sql", "classpath:integration-test-data/seed-locations-with-extended-attributes.sql")
  fun `should get non-residential appointment locations with additional room attributes`() {
    val seededVideoUUID = UUID.fromString("e58ed763-928c-4155-bee9-fdbaaadc15f3")

    locationsInsidePrisonApi().stubNonResidentialAppointmentLocationsAtPrison(WANDSWORTH, wandsworthLocation.copy(id = seededVideoUUID))

    val response = webTestClient.getAppointmentLocations(prisonCode = WANDSWORTH, requestParams = "?videoLinkOnly=false")

    assertThat(response).isNotEmpty

    with(response[0]) {
      assertThat(key).isEqualTo(wandsworthLocation.key)
      assertThat(dpsLocationId).isEqualTo(seededVideoUUID)
      assertThat(description).isEqualTo(wandsworthLocation.localName)
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
          assertThat(locationUsage).isEqualTo(LocationScheduleUsage.COURT)
        }
        with(schedule[1]) {
          assertThat(startDayOfWeek).isEqualTo(DayOfWeek.FRIDAY)
          assertThat(endDayOfWeek).isEqualTo(DayOfWeek.FRIDAY)
          assertThat(startTime).isEqualTo(LocalTime.of(0, 1))
          assertThat(endTime).isEqualTo(LocalTime.of(23, 59))
          assertThat(locationUsage).isEqualTo(LocationScheduleUsage.PROBATION)
        }
        with(schedule[2]) {
          assertThat(startDayOfWeek).isEqualTo(DayOfWeek.SATURDAY)
          assertThat(endDayOfWeek).isEqualTo(DayOfWeek.SUNDAY)
          assertThat(startTime).isEqualTo(LocalTime.of(0, 1))
          assertThat(endTime).isEqualTo(LocalTime.of(23, 59))
          assertThat(locationUsage).isEqualTo(LocationScheduleUsage.BLOCKED)
        }
      }
    }
  }

  private fun WebTestClient.getAppointmentLocations(prisonCode: String, requestParams: String = "") = get()
    .uri("/prisons/$prisonCode/locations$requestParams")
    .accept(MediaType.APPLICATION_JSON)
    .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
    .exchange()
    .expectStatus().isOk
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBodyList(Location::class.java)
    .returnResult().responseBody!!
}

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.resource

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoDate
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WERRINGTON
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.contains
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.werringtonLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.IntegrationTestBase
import java.time.LocalDate

class CsvDataExtractionIntegrationTest : IntegrationTestBase() {

  @BeforeEach
  fun before() {
    locationsInsidePrisonApi().stubVideoLinkLocationsAtPrison(
      keys = setOf(werringtonLocation.key),
      prisonCode = WERRINGTON,
    )
  }

  @Sql("classpath:integration-test-data/seed-court-events-by-hearing-date-data-extract.sql")
  @Test
  fun `should download court hearing events by hearing date`() {
    val response = webTestClient.downloadCourtDataByHearingDate(LocalDate.of(2100, 7, 24), 1)

    // This should pick up the amended event in favour of the create event.
    response contains "eventId,timestamp,videoLinkBookingId,eventType,agencyId,court,courtId,madeByTheCourt,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName\n" +
      "-1100,2024-07-24T02:00:00,-1000,AMEND,WNI,\"Derby Justice Centre\",DRBYMC,true,2100-07-25T12:00:00,2100-07-25T13:00:00,,,,,\"WNI WNI-ABCDEFG\",,"
  }

  @Sql("classpath:integration-test-data/seed-events-by-booking-date-data-extract.sql")
  @Test
  fun `should download events by booking date`() {
    val courtResponse = webTestClient.downloadCourtDataByBookingDate(LocalDate.of(2024, 1, 1), 1)

    courtResponse contains "video-links-by-court-booking-date-from-2024-01-01-for-1-days.csv"
    courtResponse contains "eventId,timestamp,videoLinkBookingId,eventType,agencyId,court,courtId,madeByTheCourt,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName\n" +
      "-2000,2024-01-01T01:00:00,-2000,CREATE,WNI,\"Derby Justice Centre\",DRBYMC,true,2099-01-24T12:00:00,2099-01-24T13:00:00,,,,,\"WNI WNI-ABCDEFG\",,"

    val probationResponse = webTestClient.downloadProbationDataByBookingDate(LocalDate.of(2024, 1, 1), 2)

    probationResponse contains "video-links-by-probation-booking-date-from-2024-01-01-for-2-days.csv"
    probationResponse contains "eventId,timestamp,videoLinkBookingId,eventType,agencyId,probationTeam,probationTeamId,madeByTheCourt,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName\n" +
      "-3000,2024-01-01T01:00:00,-3000,CREATE,WNI,\"Blackpool MC (PPOC)\",BLKPPP,true,2099-01-24T16:00:00,2099-01-24T17:00:00,,,,,\"WNI WNI-ABCDEFG\",,"
  }

  @Sql("classpath:integration-test-data/seed-probation-events-by-meeting-date-data.sql")
  @Test
  fun `should download probation events by meeting date`() {
    val probationResponse = webTestClient.downloadProbationDataByMeetingDate(LocalDate.of(2099, 1, 24), 365)

    probationResponse contains "video-links-by-probation-meeting-date-from-2099-01-24-for-365-days.csv"
    probationResponse contains "eventId,timestamp,videoLinkBookingId,eventType,agencyId,probationTeam,probationTeamId,madeByTheCourt,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName\n" +
      "-4000,2024-01-01T01:00:00,-4000,CREATE,WNI,\"Blackpool MC (PPOC)\",BLKPPP,true,2099-01-24T16:00:00,2099-01-24T17:00:00,,,,,\"WNI WNI-ABCDEFG\",,"
  }

  @Test
  fun `should be bad request when more than 365 days requested`() {
    webTestClient.badRequestWhenRequestTooManyDaysOfData()
      .expectStatus().isBadRequest
      .expectBody()
      .jsonPath("userMessage").isEqualTo("Exception: CSV extracts are limited to a years worth of data.")
      .jsonPath("developerMessage").isEqualTo("CSV extracts are limited to a years worth of data.")
  }

  private fun WebTestClient.badRequestWhenRequestTooManyDaysOfData() =
    this
      .get()
      .uri("/download-csv/court-data-by-hearing-date?start-date=2024-07-30&days=366")
      .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()

  private fun WebTestClient.downloadCourtDataByHearingDate(startDate: LocalDate, days: Long) =
    this
      .get()
      .uri("/download-csv/court-data-by-hearing-date?start-date={startDate}&days={days}", startDate.toIsoDate(), days)
      .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.parseMediaType("text/csv"))
      .expectBody()
      .returnResult()
      .toString()

  private fun WebTestClient.downloadCourtDataByBookingDate(startDate: LocalDate, days: Long) =
    this
      .get()
      .uri("/download-csv/court-data-by-booking-date?start-date={startDate}&days={days}", startDate.toIsoDate(), days)
      .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.parseMediaType("text/csv"))
      .expectBody()
      .returnResult()
      .toString()

  private fun WebTestClient.downloadProbationDataByBookingDate(startDate: LocalDate, days: Long) =
    this
      .get()
      .uri("/download-csv/probation-data-by-booking-date?start-date={startDate}&days={days}", startDate.toIsoDate(), days)
      .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.parseMediaType("text/csv"))
      .expectBody()
      .returnResult()
      .toString()

  private fun WebTestClient.downloadProbationDataByMeetingDate(startDate: LocalDate, days: Long) =
    this
      .get()
      .uri("/download-csv/probation-data-by-meeting-date?start-date={startDate}&days={days}", startDate.toIsoDate(), days)
      .accept(MediaType.parseMediaType("text/csv"), MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_BOOK_A_VIDEO_LINK_ADMIN")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType(MediaType.parseMediaType("text/csv"))
      .expectBody()
      .returnResult()
      .toString()
}

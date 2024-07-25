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

  @Sql("classpath:integration-test-data/seed-court-hearing-by-hearing-date-data-extract.sql")
  @Test
  fun `should download court hearing events by hearing date`() {
    val response = webTestClient.downloadCourtDataByHearingDate(LocalDate.of(2100, 7, 24), 1)

    // This should pick up the amended event in favour of the create event.
    response.toString() contains "timestamp,videoLinkBookingId,eventType,agencyId,court,courtId,madeByTheCourt,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName\n" +
      "2024-07-24T02:00:00,-1000,AMEND,WNI,\"Derby Justice Centre\",DRBYMC,true,2100-07-25T12:00:00,2100-07-25T13:00:00,,,,,\"WNI WNI-ABCDEFG\",,"
  }

  @Sql("classpath:integration-test-data/seed-court-hearing-by-booking-date-data-extract.sql")
  @Test
  fun `should download court hearing events by booking date`() {
    val response = webTestClient.downloadCourtDataByBookingDate(LocalDate.of(2024, 1, 1), 1)

    response.toString() contains "timestamp,videoLinkBookingId,eventType,agencyId,court,courtId,madeByTheCourt,mainStartTime,mainEndTime,preStartTime,preEndTime,postStartTime,postEndTime,mainLocationName,preLocationName,postLocationName\n" +
      "2024-01-01T01:00:00,-2000,CREATE,WNI,\"Derby Justice Centre\",DRBYMC,true,2099-01-24T12:00:00,2099-01-24T13:00:00,,,,,\"WNI WNI-ABCDEFG\",,"
  }

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
}

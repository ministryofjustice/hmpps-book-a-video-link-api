package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.officialvisits

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.officialvisits.model.OfficialVisitSummarySearchResponse
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.officialvisits.model.PrisonerVisitedDetails
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.officialvisits.model.VisitStatusType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.officialvisits.model.VisitType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toHourMinuteStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactly
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvilleLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.pentonvillePrisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.yesterday
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.OfficialVisitsApiMockServer
import java.time.LocalTime

class OfficialVisitsClientTest {
  private val server = OfficialVisitsApiMockServer().also { it.start() }
  private val client = OfficialVisitsClient(WebClient.create("http://localhost:${server.port()}"))

  @Test
  fun `should find official visits`() {
    val searchResponse = OfficialVisitSummarySearchResponse(
      officialVisitId = 1,
      prisonCode = PENTONVILLE,
      prisonDescription = "prison description",
      visitStatus = VisitStatusType.SCHEDULED,
      visitStatusDescription = "visit status description",
      visitTypeCode = VisitType.VIDEO,
      visitTypeDescription = "visit type description",
      visitDate = today(),
      startTime = LocalTime.of(10, 0).toHourMinuteStyle(),
      endTime = LocalTime.of(11, 0).toHourMinuteStyle(),
      dpsLocationId = pentonvilleLocation.id,
      locationDescription = "location description",
      visitSlotId = 1,
      numberOfVisitors = 1,
      createdBy = "a user",
      createdTime = yesterday().atStartOfDay(),
      prisoner = PrisonerVisitedDetails(
        prisonCode = PENTONVILLE,
        prisonerNumber = pentonvillePrisoner.number,
      ),
      visitorIssues = false,
    )

    server.stubPostFindOfficialVisitsBy(PENTONVILLE, today(), tomorrow(), searchResponse)

    client.findOfficialVisits(PENTONVILLE, today(), tomorrow()) containsExactly listOf(searchResponse)
  }

  @AfterEach
  fun after() {
    server.stop()
  }
}

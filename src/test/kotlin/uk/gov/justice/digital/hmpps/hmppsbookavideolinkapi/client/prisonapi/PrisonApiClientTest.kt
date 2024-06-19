package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.PrisonApiMockServer
import java.time.LocalDate
import java.time.LocalTime

class PrisonApiClientTest {

  private val server = PrisonApiMockServer().also { it.start() }
  private val client = PrisonApiClient(WebClient.create("http://localhost:${server.port()}"))

  @Test
  fun `should get location by key`() {
    server.stubGetInternalLocationByKey("key", BIRMINGHAM, "A-123")

    client.getInternalLocationByKey("key") isEqualTo Location(
      locationId = 1,
      locationType = "VIDEO_LINK",
      agencyId = BIRMINGHAM,
      description = "A-123",
    )
  }

  @Test
  fun `should post appointment`() {
    server.stubPostCreateAppointment(
      bookingId = 1,
      appointmentType = "VLB",
      locationId = 2,
      appointmentDate = LocalDate.now(),
      startTime = LocalTime.MIDNIGHT,
      endTime = LocalTime.MIDNIGHT.plusHours(1),
    )

    val response = client.createAppointment(
      bookingId = 1,
      locationId = 2,
      appointmentDate = LocalDate.now(),
      startTime = LocalTime.MIDNIGHT,
      endTime = LocalTime.MIDNIGHT.plusHours(1),
    )

    assertThat(response).isNotNull
  }

  @AfterEach
  fun after() {
    server.stop()
  }
}

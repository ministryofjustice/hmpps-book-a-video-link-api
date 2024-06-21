package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.ActivitiesAppointmentsApiMockServer
import java.time.LocalTime

class ActivitiesAppointmentsClientTest {

  private val server = ActivitiesAppointmentsApiMockServer().also { it.start() }
  private val client = ActivitiesAppointmentsClient(WebClient.create("http://localhost:${server.port()}"))

  @Test
  fun `should post new appointment`() {
    server.stubPostCreateAppointment(
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      startDate = tomorrow(),
      startTime = LocalTime.MIDNIGHT,
      endTime = LocalTime.MIDNIGHT.plusHours(1),
      internalLocationId = 1,
      extraInformation = "extra info",
    )

    client.createAppointment(
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      startDate = tomorrow(),
      startTime = LocalTime.MIDNIGHT,
      endTime = LocalTime.MIDNIGHT.plusHours(1),
      internalLocationId = 1,
      comments = "extra info",
    )
  }

  @Test
  fun `should get prisoners appointments`() {
    server.stubGetPrisonersAppointments(BIRMINGHAM, "123456", tomorrow())

    assertThat(client.getPrisonersAppointments(BIRMINGHAM, "123456", tomorrow())).isNotEmpty
  }

  @AfterEach
  fun after() {
    server.stop()
  }
}

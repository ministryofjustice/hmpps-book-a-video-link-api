package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.ActivitiesAppointmentsApiMockServer

class ActivitiesAppointmentsClientTest {

  private val server = ActivitiesAppointmentsApiMockServer().also { it.start() }
  private val client = ActivitiesAppointmentsClient(WebClient.create("http://localhost:${server.port()}"))

  @Test
  fun `should get matching appointment`() {
    server.stubGetAppointment(1L)

    client.getAppointment(1L)?.id isEqualTo 1L
  }

  @AfterEach
  fun after() {
    server.stop()
  }
}

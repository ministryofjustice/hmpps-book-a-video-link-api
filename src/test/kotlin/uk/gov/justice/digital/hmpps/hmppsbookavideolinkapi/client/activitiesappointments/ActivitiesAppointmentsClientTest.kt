package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.RISLEY
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
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
  fun `should get single prisoners appointment`() {
    server.stubGetPrisonersAppointments(
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      date = tomorrow(),
      locationIds = setOf(1000, 2000),
    )

    val appointment = client.getPrisonersAppointmentsAtLocations(BIRMINGHAM, "123456", tomorrow(), 1000).single()

    appointment.internalLocation?.id isEqualTo 1000
  }

  @Test
  fun `should get multiple prisoner appointments at locations`() {
    server.stubGetPrisonersAppointments(
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      date = tomorrow(),
      locationIds = setOf(1000, 2000, 3000),
    )

    val appointments = client.getPrisonersAppointmentsAtLocations(BIRMINGHAM, "123456", tomorrow(), 2000, 3000)

    appointments hasSize 2
    appointments.map { it.internalLocation?.id } containsExactlyInAnyOrder setOf(2000, 3000)
  }

  @Test
  fun `should get no prisoner appointments at locations when not video link locations`() {
    server.stubGetPrisonersAppointments(
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      date = tomorrow(),
      locationType = "NOT_VLB",
      locationIds = setOf(1000, 2000, 3000),
    )

    val appointments = client.getPrisonersAppointmentsAtLocations(BIRMINGHAM, "123456", tomorrow(), 1000, 2000, 3000)

    appointments hasSize 0
  }

  @Test
  fun `should get no prisoner appointments at locations`() {
    server.stubGetPrisonersAppointments(
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      date = tomorrow(),
      locationIds = setOf(1000, 2000, 3000),
    )

    client.getPrisonersAppointmentsAtLocations(BIRMINGHAM, "123456", tomorrow(), 4000) hasSize 0
  }

  @Test
  fun `should be rolled out prison`() {
    server.stubGetRolledOutPrison(PENTONVILLE, true)
    client.isAppointmentsRolledOutAt(PENTONVILLE) isBool true

    server.stubGetRolledOutPrison(RISLEY, true)
    client.isAppointmentsRolledOutAt(RISLEY) isBool true
  }

  @Test
  fun `should not be rolled out prison`() {
    server.stubGetRolledOutPrison(PENTONVILLE, false)
    client.isAppointmentsRolledOutAt(PENTONVILLE) isBool false

    server.stubGetRolledOutPrison(RISLEY, false)
    client.isAppointmentsRolledOutAt(RISLEY) isBool false
  }

  @AfterEach
  fun after() {
    server.stop()
  }
}

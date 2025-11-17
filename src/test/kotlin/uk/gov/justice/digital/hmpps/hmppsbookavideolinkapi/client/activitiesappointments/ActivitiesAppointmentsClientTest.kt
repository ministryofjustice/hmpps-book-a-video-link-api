package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Spy
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.SupportedAppointmentTypes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.RISLEY
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isInstanceOf
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.ActivitiesAppointmentsApiMockServer
import java.time.LocalTime
import java.util.UUID

class ActivitiesAppointmentsClientTest {

  @Spy
  private val server = ActivitiesAppointmentsApiMockServer().also { it.start() }
  private val client = ActivitiesAppointmentsClient(WebClient.create("http://localhost:${server.port()}"))

  @Test
  fun `should post new appointment`() {
    val dpsLocationId = UUID.fromString("00000000-0000-0000-0000-000000000000")

    server.stubPostCreateAppointment(
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      startDate = tomorrow(),
      startTime = LocalTime.MIDNIGHT,
      endTime = LocalTime.MIDNIGHT.plusHours(1),
      dpsLocationsId = dpsLocationId,
      extraInformation = "extra info",
      prisonerExtraInformation = "prisoner extra info",
      appointmentType = SupportedAppointmentTypes.Type.COURT,
    )

    val response = client.createAppointment(
      prisonCode = BIRMINGHAM,
      prisonerNumber = "123456",
      startDate = tomorrow(),
      startTime = LocalTime.MIDNIGHT,
      endTime = LocalTime.MIDNIGHT.plusHours(1),
      dpsLocationId = dpsLocationId,
      extraInformation = "extra info",
      prisonerExtraInformation = "prisoner extra info",
      appointmentType = SupportedAppointmentTypes.Type.COURT,
    )

    response isInstanceOf AppointmentSeries::class.java
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

  @Test
  fun `should correct cancellation reason on cancel with delete`() {
    client.cancelAppointment(1, true)
  }

  @AfterEach
  fun after() {
    server.stop()
  }
}

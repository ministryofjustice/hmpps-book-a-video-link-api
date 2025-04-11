package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.model.Movement
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.SupportedAppointmentTypes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactly
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactlyInAnyOrder
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.hasSize
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isEqualTo
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.today
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.tomorrow
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock.PrisonApiMockServer
import java.time.LocalDate
import java.time.LocalTime

class PrisonApiClientTest {

  private val server = PrisonApiMockServer().also { it.start() }
  private val client = PrisonApiClient(WebClient.create("http://localhost:${server.port()}"))

  @Test
  fun `should post appointment`() {
    server.stubPostCreateAppointment(
      bookingId = 1,
      appointmentType = SupportedAppointmentTypes.Type.COURT,
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
      appointmentType = SupportedAppointmentTypes.Type.COURT,
    )

    assertThat(response).isNotNull
  }

  @Test
  fun `should get single prisoner appointment at location`() {
    server.stubGetPrisonersAppointments(prisonCode = BIRMINGHAM, prisonerNumber = "123456", date = tomorrow(), locationIds = setOf(1000, 2000, 3000))

    val appointment = client.getPrisonersAppointmentsAtLocations(BIRMINGHAM, "123456", tomorrow(), 2000).single()

    appointment.locationId isEqualTo 2000
  }

  @Test
  fun `should get multiple prisoner appointments at locations`() {
    server.stubGetPrisonersAppointments(prisonCode = BIRMINGHAM, prisonerNumber = "123456", date = tomorrow(), locationIds = setOf(1000, 2000, 3000))

    val appointments = client.getPrisonersAppointmentsAtLocations(BIRMINGHAM, "123456", tomorrow(), 2000, 3000)

    appointments hasSize 2
    appointments.map { it.locationId } containsExactlyInAnyOrder setOf(2000, 3000)
  }

  @Test
  fun `should get no prisoner appointments at locations`() {
    server.stubGetPrisonersAppointments(prisonCode = BIRMINGHAM, prisonerNumber = "123456", date = tomorrow(), locationIds = setOf(1000, 2000, 3000))

    client.getPrisonersAppointmentsAtLocations(BIRMINGHAM, "123456", tomorrow(), 4000) hasSize 0
  }

  @Test
  fun `should get unfiltered scheduled appointments`() {
    server.stubGetScheduledAppointments(prisonCode = BIRMINGHAM, date = tomorrow(), setOf(1000, 2000, 3000))

    client.getScheduledAppointments(BIRMINGHAM, tomorrow(), emptySet()) hasSize 3
  }

  @Test
  fun `should get filtered scheduled appointments`() {
    server.stubGetScheduledAppointments(prisonCode = BIRMINGHAM, date = tomorrow(), setOf(1000, 2000, 3000))

    client.getScheduledAppointments(BIRMINGHAM, tomorrow(), setOf(1000)).map { it.locationId } containsExactly listOf(1000)
    client.getScheduledAppointments(BIRMINGHAM, tomorrow(), setOf(1000, 3000)).map { it.locationId } containsExactlyInAnyOrder listOf(1000, 3000)
    client.getScheduledAppointments(BIRMINGHAM, tomorrow(), setOf(2000, 3000)).map { it.locationId } containsExactlyInAnyOrder listOf(2000, 3000)
  }

  @Test
  fun `should get latest prisoner movement type`() {
    server.stubGetLatestPrisonerMovement("123456", today(), Movement.MovementType.TRN)

    client.getLatestPrisonerMovementOnDate("123456", today()) isEqualTo Movement.MovementType.TRN
  }

  @Test
  fun `should be null latest prisoner movement type for future date`() {
    server.stubGetLatestPrisonerMovement("123456", today(), Movement.MovementType.TRN)

    client.getLatestPrisonerMovementOnDate("123456", tomorrow()) isEqualTo null
  }

  @AfterEach
  fun after() {
    server.stop()
  }
}

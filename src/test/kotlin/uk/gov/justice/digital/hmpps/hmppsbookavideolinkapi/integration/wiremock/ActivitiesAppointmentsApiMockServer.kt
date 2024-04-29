package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentAttendee
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ActivitiesAppointmentsApiMockServer : MockServer(8089) {

  fun stubGetAppointment(appointmentId: Long) {
    stubFor(
      get("/appointments/$appointmentId")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                Appointment(
                  id = appointmentId,
                  sequenceNumber = 1,
                  prisonCode = "MDI",
                  categoryCode = "VIDE",
                  createdBy = "Fred",
                  createdTime = LocalDateTime.now(),
                  inCell = false,
                  isCancelled = false,
                  isDeleted = false,
                  startDate = LocalDate.now(),
                  startTime = LocalTime.now().toString(),
                  attendees = listOf(
                    AppointmentAttendee(
                      id = 1,
                      prisonerNumber = "123456",
                      bookingId = 1,
                    ),
                  ),
                ),
              ),
            )
            .withStatus(200),
        ),
    )
  }
}

class ActivitiesAppointmentsApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val server = ActivitiesAppointmentsApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    server.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    server.resetAll()
  }

  override fun afterAll(context: ExtensionContext) {
    server.stop()
  }
}

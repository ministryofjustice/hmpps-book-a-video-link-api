package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.VIDEO_LINK_BOOKING
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentAttendee
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentSeriesCreateRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toHourMinuteStyle
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

  fun stubPostCreateAppointment(
    prisonCode: String,
    prisonerNumber: String,
    startDate: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime,
    internalLocationId: Long,
    extraInformation: String,
  ) {
    val request = AppointmentSeriesCreateRequest(
      appointmentType = AppointmentSeriesCreateRequest.AppointmentType.INDIVIDUAL,
      prisonCode = prisonCode,
      prisonerNumbers = listOf(prisonerNumber),
      categoryCode = VIDEO_LINK_BOOKING,
      tierCode = AppointmentSeriesCreateRequest.TierCode.TIER_1,
      inCell = false,
      startDate = startDate,
      startTime = startTime.toHourMinuteStyle(),
      endTime = endTime.toHourMinuteStyle(),
      internalLocationId = internalLocationId,
      extraInformation = extraInformation,
    )

    stubFor(
      post("/appointment-series")
        .withRequestBody(WireMock.equalToJson(mapper.writeValueAsString(request)))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                AppointmentSeries(
                  id = 1,
                  appointmentType = AppointmentSeries.AppointmentType.valueOf(request.appointmentType.value),
                  prisonCode = request.prisonCode,
                  categoryCode = request.categoryCode,
                  inCell = request.inCell,
                  startDate = request.startDate,
                  startTime = request.startTime,
                  endTime = request.endTime,
                  createdBy = "Test",
                  createdTime = LocalDateTime.now(),
                  appointments = listOf(
                    Appointment(
                      id = 1,
                      sequenceNumber = 1,
                      startDate = request.startDate,
                      startTime = request.startTime,
                      endTime = request.endTime,
                      attendees = listOf(
                        AppointmentAttendee(
                          id = -1,
                          prisonerNumber = request.prisonerNumbers.single(),
                          bookingId = -1,
                          addedTime = LocalDateTime.now(),
                          addedBy = "test",
                        ),
                      ),
                      categoryCode = request.categoryCode,
                      inCell = request.inCell,
                      prisonCode = request.prisonCode,
                      isCancelled = false,
                      isDeleted = false,
                      createdBy = "Test",
                      createdTime = LocalDateTime.now(),
                    ),
                  ),
                ),
              ),
            )
            .withStatus(201),
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

package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.ScheduledEvent
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.VIDEO_LINK_BOOKING
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.model.NewAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoDate
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoDateTime
import java.time.LocalDate
import java.time.LocalTime

class PrisonApiMockServer : MockServer(8094) {

  fun stubGetInternalLocationByKey(key: String, prisonCode: String, description: String = "location description") {
    stubFor(
      get("/api/locations/code/$key")
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                Location(
                  locationId = 1,
                  locationType = "VIDEO_LINK",
                  agencyId = prisonCode,
                  description = description,
                ),
              ),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubPostCreateAppointment(
    bookingId: Long,
    appointmentType: String,
    locationId: Long,
    appointmentDate: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime,
    comments: String? = null,
  ) {
    stubFor(
      post("/api/bookings/$bookingId/appointments")
        .withRequestBody(
          WireMock.equalToJson(
            mapper.writeValueAsString(
              NewAppointment(
                appointmentType = appointmentType,
                locationId = locationId,
                startTime = appointmentDate.atTime(startTime).toIsoDateTime(),
                endTime = appointmentDate.atTime(endTime).toIsoDateTime(),
                comment = comments,
              ),
            ),
          ),
        )
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(ScheduledEvent(eventId = 1)),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubGetPrisonersAppointments(
    prisonCode: String,
    prisonerNumber: String,
    date: LocalDate,
    locationType: String = VIDEO_LINK_BOOKING,
    locationIds: Set<Long> = setOf(-1),
  ) {
    val locations = locationIds.map { locationId ->
      PrisonerSchedule(
        offenderNo = "G5662GI",
        locationId = locationId,
        firstName = "JOHN",
        lastName = "DOE",
        event = locationType,
        startTime = date.atStartOfDay(),
        endTime = date.atStartOfDay().plusHours(1),
        eventId = 1,
      )
    }

    stubFor(
      post("/api/schedules/$prisonCode/appointments?date=${date.toIsoDate()}")
        .withRequestBody(
          WireMock.equalToJson(
            mapper.writeValueAsString(listOf(prisonerNumber)),
          ),
        )
        .willReturn(
          WireMock.aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(locations),
            )
            .withStatus(200),
        ),
    )
  }
}

class PrisonApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val server = PrisonApiMockServer()
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

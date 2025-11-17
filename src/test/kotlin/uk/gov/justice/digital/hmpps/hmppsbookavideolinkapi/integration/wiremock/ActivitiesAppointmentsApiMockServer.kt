package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.integration.wiremock

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.Appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentAttendee
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentAttendeeSearchResult
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentSearchRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentSearchResult
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentSeries
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentSeriesCreateRequest
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.RolloutPrisonPlan
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.SupportedAppointmentTypes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toHourMinuteStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toIsoDateTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class ActivitiesAppointmentsApiMockServer : MockServer(8089) {

  fun stubPostCreateAppointment(
    prisonCode: String,
    prisonerNumber: String,
    startDate: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime,
    dpsLocationsId: UUID,
    extraInformation: String,
    prisonerExtraInformation: String,
    appointmentType: SupportedAppointmentTypes.Type,
  ) {
    val request = AppointmentSeriesCreateRequest(
      appointmentType = AppointmentSeriesCreateRequest.AppointmentType.INDIVIDUAL,
      prisonCode = prisonCode,
      prisonerNumbers = listOf(prisonerNumber),
      categoryCode = appointmentType.code,
      tierCode = AppointmentSeriesCreateRequest.TierCode.TIER_1,
      inCell = false,
      startDate = startDate,
      startTime = startTime.toHourMinuteStyle(),
      endTime = endTime.toHourMinuteStyle(),
      dpsLocationId = dpsLocationsId,
      extraInformation = extraInformation,
      prisonerExtraInformation = prisonerExtraInformation,
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
                  dpsLocationId = dpsLocationsId,
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

  fun stubGetPrisonersAppointments(
    prisonCode: String,
    prisonerNumber: String,
    date: LocalDate,
    appointmentType: SupportedAppointmentTypes.Type = SupportedAppointmentTypes.Type.COURT,
    locationIds: Set<Long> = setOf(-1),
  ) {
    val appointments = locationIds.map { locationId ->
      AppointmentSearchResult(
        appointmentType = AppointmentSearchResult.AppointmentType.INDIVIDUAL,
        startDate = date,
        startTime = date.atStartOfDay().toIsoDateTime(),
        endTime = date.atStartOfDay().plusHours(1).toIsoDateTime(),
        isCancelled = false,
        isExpired = false,
        isEdited = false,
        appointmentId = 1,
        appointmentSeriesId = 1,
        appointmentName = "appointment name",
        attendees = listOf(AppointmentAttendeeSearchResult(1, prisonerNumber, 1)),
        category = AppointmentCategorySummary(appointmentType.code, appointmentType.name),
        inCell = false,
        isRepeat = false,
        maxSequenceNumber = 1,
        prisonCode = prisonCode,
        sequenceNumber = 1,
        internalLocation = AppointmentLocationSummary(locationId, prisonCode, "VIDEO LINK"),
        timeSlot = AppointmentSearchResult.TimeSlot.AM,
        createdTime = LocalDateTime.now(),
        isDeleted = false,
        customName = null,
        updatedTime = null,
        cancelledTime = null,
        cancelledBy = null,
      )
    }

    stubFor(
      post("/appointments/$prisonCode/search")
        .withRequestBody(
          WireMock.equalToJson(
            mapper.writeValueAsString(
              AppointmentSearchRequest(
                startDate = date,
                prisonerNumbers = listOf(prisonerNumber),
              ),
            ),
          ),
        )
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(mapper.writeValueAsString(appointments))
            .withStatus(201),
        ),
    )
  }

  fun stubGetRolledOutPrison(prisonCode: String, isLive: Boolean) {
    stubFor(
      WireMock.get("/rollout/$prisonCode")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              mapper.writeValueAsString(
                RolloutPrisonPlan(
                  prisonCode = prisonCode,
                  activitiesRolledOut = true,
                  appointmentsRolledOut = true,
                  maxDaysToExpiry = 1,
                  prisonLive = isLive,
                ),
              ),
            )
            .withStatus(200),
        ),
    )
  }
}

class ActivitiesAppointmentsApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
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

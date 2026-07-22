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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PENTONVILLE
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

  fun stubGetUncancelledVideoAppointments(prisonCode: String, fromDate: LocalDate, toDate: LocalDate) {
    val locationUuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")

    val appointments = listOf(
      appointmentResult(
        locationId = locationUuid,
        appointmentSeriesId = 1L,
        appointmentId = 1L,
        date = fromDate,
        startTime = "09:00",
        endTime = "10:00",
        categoryCode = "VLOO",
        categoryDescription = "Video link - official other",
      ),
      appointmentResult(
        locationId = locationUuid,
        appointmentSeriesId = 2L,
        appointmentId = 2L,
        date = fromDate,
        startTime = "10:00",
        endTime = "11:00",
        categoryCode = "VLLA",
        categoryDescription = "Video link - legal appointment",
      ),
      appointmentResult(
        locationId = locationUuid,
        appointmentSeriesId = 3L,
        appointmentId = 3L,
        date = fromDate,
        startTime = "11:15",
        endTime = "12:00",
        categoryCode = "VLAP",
        categoryDescription = "Video link - another prison",
        isCancelled = true,
      ),
      appointmentResult(
        locationId = locationUuid,
        appointmentSeriesId = 4L,
        appointmentId = 4L,
        date = fromDate,
        startTime = "11:30",
        endTime = "12:00",
        categoryCode = "VLPA",
        categoryDescription = "Video link - parole",
        isDeleted = true,
      ),
      appointmentResult(
        locationId = locationUuid,
        appointmentSeriesId = 5L,
        appointmentId = 5L,
        date = fromDate,
        startTime = "12:00",
        endTime = "13:00",
        categoryCode = "VLB",
        categoryDescription = "Video link - court hearing",
      ),
      appointmentResult(
        locationId = locationUuid,
        appointmentSeriesId = 6L,
        appointmentId = 6L,
        date = fromDate,
        startTime = "13:00",
        endTime = "14:00",
        categoryCode = "VLPM",
        categoryDescription = "Video link - probation meeting",
      ),
      appointmentResult(
        locationId = locationUuid,
        appointmentSeriesId = 7L,
        appointmentId = 7L,
        date = fromDate,
        startTime = "14:00",
        endTime = "15:00",
        categoryCode = "CHAP",
        categoryDescription = "Chaplaincy",
      ),
    )

    stubFor(
      post("/appointments/$prisonCode/search")
        .withRequestBody(
          WireMock.equalToJson(
            mapper.writeValueAsString(
              AppointmentSearchRequest(
                startDate = fromDate,
                endDate = toDate,
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

fun appointmentResult(
  prisonCode: String = PENTONVILLE,
  prisonerNumber: String = "G4195VF",
  locationId: UUID,
  appointmentType: AppointmentSearchResult.AppointmentType = AppointmentSearchResult.AppointmentType.INDIVIDUAL,
  appointmentSeriesId: Long,
  appointmentId: Long,
  categoryCode: String,
  categoryDescription: String,
  date: LocalDate = LocalDate.now(),
  startTime: String,
  endTime: String,
  isCancelled: Boolean = false,
  isDeleted: Boolean = false,
) = AppointmentSearchResult(
  appointmentType = appointmentType,
  startDate = date,
  startTime = startTime,
  endTime = endTime,
  isCancelled = isCancelled,
  isExpired = false,
  isEdited = false,
  appointmentId = appointmentId,
  appointmentSeriesId = appointmentSeriesId,
  appointmentName = "",
  attendees = listOf(AppointmentAttendeeSearchResult(1, prisonerNumber, 1)),
  category = AppointmentCategorySummary(categoryCode, categoryDescription),
  inCell = false,
  isRepeat = false,
  maxSequenceNumber = 1,
  prisonCode = prisonCode,
  sequenceNumber = 1,
  internalLocation = AppointmentLocationSummary(1L, prisonCode, "VIDEO LINK", dpsLocationId = locationId),
  timeSlot = AppointmentSearchResult.TimeSlot.AM,
  createdTime = LocalDateTime.now(),
  isDeleted = isDeleted,
  customName = null,
  updatedTime = null,
  cancelledTime = null,
  cancelledBy = null,
)

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

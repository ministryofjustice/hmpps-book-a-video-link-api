package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.ActivitiesAppointmentsClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentAttendeeSearchResult
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentSearchResult
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.SupportedAppointmentTypes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toHourMinuteStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactly
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NomisMappingService
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

private const val VLB = "VLB"
private const val VLOO = "VLOO"
private const val VLPM = "VLPM"

class ActivitiesAndAppointmentsServiceTest {
  private val activitiesAppointmentsClient: ActivitiesAppointmentsClient = mock()
  private val nomisMappingService: NomisMappingService = mock()
  private val supportedAppointmentTypes = SupportedAppointmentTypes()
  private val service = ActivitiesAndAppointmentsService(activitiesAppointmentsClient, nomisMappingService, supportedAppointmentTypes)

  private val birminghamLocation = Location(
    locationId = 123456,
    locationType = VLB,
    "VIDEO LINK",
    BIRMINGHAM,
  )
  private val courtBooking = courtBooking()
  private val courtAppointment = appointment(
    booking = courtBooking,
    prisonCode = BIRMINGHAM,
    prisonerNumber = "123456",
    appointmentType = "VLB_COURT_PRE",
    date = LocalDate.of(2100, 1, 1),
    startTime = LocalTime.of(11, 0),
    endTime = LocalTime.of(11, 30),
    locationId = UUID.randomUUID(),
  )
  private val probationBooking = probationBooking()
  private val probationAppointment = appointment(
    booking = probationBooking,
    prisonCode = BIRMINGHAM,
    prisonerNumber = "654321",
    appointmentType = "VLB_PROBATION",
    date = LocalDate.of(2100, 1, 1),
    startTime = LocalTime.of(11, 0),
    endTime = LocalTime.of(11, 30),
    locationId = UUID.randomUUID(),
  )

  @Nested
  @DisplayName("Map correct appointment type for bookings when probation VLPM feature is live")
  inner class MapToCourtAndProbationAppointmentType {

    @Test
    fun `should create VLB court appointment`() {
      whenever(nomisMappingService.getNomisLocationId(courtAppointment.prisonLocationId)) doReturn birminghamLocation.locationId

      service.createAppointment(courtAppointment)

      verify(nomisMappingService).getNomisLocationId(courtAppointment.prisonLocationId)
      verify(activitiesAppointmentsClient).createAppointment(
        prisonCode = BIRMINGHAM,
        prisonerNumber = "123456",
        startDate = LocalDate.of(2100, 1, 1),
        startTime = LocalTime.of(11, 0),
        endTime = LocalTime.of(11, 30),
        internalLocationId = 123456,
        comments = "Court hearing comments",
        appointmentType = SupportedAppointmentTypes.Type.COURT,
      )
    }

    @Test
    fun `should create VLPM probation appointment`() {
      whenever(nomisMappingService.getNomisLocationId(probationAppointment.prisonLocationId)) doReturn birminghamLocation.locationId

      service.createAppointment(probationAppointment)

      verify(nomisMappingService).getNomisLocationId(probationAppointment.prisonLocationId)
      verify(activitiesAppointmentsClient).createAppointment(
        prisonCode = BIRMINGHAM,
        prisonerNumber = "654321",
        startDate = LocalDate.of(2100, 1, 1),
        startTime = LocalTime.of(11, 0),
        endTime = LocalTime.of(11, 30),
        internalLocationId = 123456,
        comments = "Probation meeting comments",
        appointmentType = SupportedAppointmentTypes.Type.PROBATION,
      )
    }
  }

  @Nested
  @DisplayName("Matching appointments")
  inner class MatchingAppointments {
    @Test
    fun `should find matching court VLB appointment`() {
      val matchingAppointment = appointmentSearchResult(courtAppointment, birminghamLocation, VLB)

      whenever(
        activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
          prisonCode = courtAppointment.prisonCode(),
          prisonerNumber = courtAppointment.prisonerNumber,
          onDate = courtAppointment.appointmentDate,
          birminghamLocation.locationId,
        ),
      ) doReturn listOf(matchingAppointment)

      whenever(nomisMappingService.getNomisLocationId(courtAppointment.prisonLocationId)) doReturn birminghamLocation.locationId

      service.findMatchingAppointments(courtAppointment) containsExactly listOf(matchingAppointment.appointmentId)
    }

    @Test
    fun `should find matching probation VLB appointment`() {
      val matchingAppointment = appointmentSearchResult(probationAppointment, birminghamLocation, VLB)

      whenever(
        activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
          prisonCode = probationAppointment.prisonCode(),
          prisonerNumber = probationAppointment.prisonerNumber,
          onDate = probationAppointment.appointmentDate,
          birminghamLocation.locationId,
        ),
      ) doReturn listOf(matchingAppointment)

      whenever(nomisMappingService.getNomisLocationId(probationAppointment.prisonLocationId)) doReturn birminghamLocation.locationId

      service.findMatchingAppointments(probationAppointment) containsExactly listOf(matchingAppointment.appointmentId)
    }

    @Test
    fun `should find matching VLPM appointment`() {
      val matchingAppointment = appointmentSearchResult(probationAppointment, birminghamLocation, VLPM)

      whenever(
        activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
          prisonCode = probationAppointment.prisonCode(),
          prisonerNumber = probationAppointment.prisonerNumber,
          onDate = probationAppointment.appointmentDate,
          birminghamLocation.locationId,
        ),
      ) doReturn listOf(matchingAppointment)

      whenever(nomisMappingService.getNomisLocationId(probationAppointment.prisonLocationId)) doReturn birminghamLocation.locationId

      service.findMatchingAppointments(probationAppointment) containsExactly listOf(matchingAppointment.appointmentId)
    }

    @Test
    fun `should not find matching appointment when already cancelled`() {
      val cancelledAppointment = appointmentSearchResult(courtAppointment, birminghamLocation, VLB).copy(isCancelled = true)

      whenever(
        activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
          prisonCode = courtAppointment.prisonCode(),
          prisonerNumber = courtAppointment.prisonerNumber,
          onDate = courtAppointment.appointmentDate,
          birminghamLocation.locationId,
        ),
      ) doReturn listOf(cancelledAppointment)

      whenever(nomisMappingService.getNomisLocationId(courtAppointment.prisonLocationId)) doReturn birminghamLocation.locationId

      service.findMatchingAppointments(courtAppointment).isEmpty() isBool true
    }

    @Test
    fun `should not find matching appointment when times do not match`() {
      val differentEndTimeAppointment = appointmentSearchResult(courtAppointment, birminghamLocation, VLB).copy(endTime = courtAppointment.endTime.plusHours(1).toHourMinuteStyle())

      whenever(
        activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
          prisonCode = courtAppointment.prisonCode(),
          prisonerNumber = courtAppointment.prisonerNumber,
          onDate = courtAppointment.appointmentDate,
          birminghamLocation.locationId,
        ),
      ) doReturn listOf(differentEndTimeAppointment)

      whenever(nomisMappingService.getNomisLocationId(courtAppointment.prisonLocationId)) doReturn birminghamLocation.locationId

      service.findMatchingAppointments(courtAppointment).isEmpty() isBool true
    }

    @Test
    fun `should not find matching appointment when unsupported VLOO appointment type`() {
      val vlooAppointment = appointmentSearchResult(courtAppointment, birminghamLocation, VLOO)

      whenever(
        activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
          prisonCode = courtAppointment.prisonCode(),
          prisonerNumber = courtAppointment.prisonerNumber,
          onDate = courtAppointment.appointmentDate,
          birminghamLocation.locationId,
        ),
      ) doReturn listOf(vlooAppointment)

      whenever(nomisMappingService.getNomisLocationId(courtAppointment.prisonLocationId)) doReturn birminghamLocation.locationId

      service.findMatchingAppointments(probationAppointment).isEmpty() isBool true
    }

    @Test
    fun `should not find matching VLPM appointment`() {
      val vlpmAppointment = appointmentSearchResult(probationAppointment, birminghamLocation, VLPM)

      whenever(
        activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
          prisonCode = probationAppointment.prisonCode(),
          prisonerNumber = probationAppointment.prisonerNumber,
          onDate = probationAppointment.appointmentDate,
          birminghamLocation.locationId,
        ),
      ) doReturn listOf(vlpmAppointment)

      whenever(nomisMappingService.getNomisLocationId(probationAppointment.prisonLocationId)) doReturn birminghamLocation.locationId

      service.findMatchingAppointments(probationAppointment) containsExactly listOf(vlpmAppointment.appointmentId)
    }
  }

  private fun appointmentSearchResult(appointment: PrisonAppointment, location: Location, appointmentType: String) = run {
    AppointmentSearchResult(
      appointmentType = AppointmentSearchResult.AppointmentType.INDIVIDUAL,
      startDate = appointment.appointmentDate,
      startTime = appointment.startTime.toHourMinuteStyle(),
      endTime = appointment.endTime.toHourMinuteStyle(),
      isCancelled = false,
      isExpired = false,
      isEdited = false,
      appointmentId = 99,
      appointmentSeriesId = 1,
      appointmentName = "appointment name",
      attendees = listOf(AppointmentAttendeeSearchResult(1, appointment.prisonerNumber, 1)),
      category = AppointmentCategorySummary(appointmentType, "video link booking"),
      inCell = false,
      isRepeat = false,
      maxSequenceNumber = 1,
      prisonCode = appointment.prisonCode(),
      sequenceNumber = 1,
      internalLocation = AppointmentLocationSummary(
        location.locationId,
        appointment.prisonCode(),
        "VIDEO LINK",
      ),
      timeSlot = AppointmentSearchResult.TimeSlot.AM,
    )
  }
}

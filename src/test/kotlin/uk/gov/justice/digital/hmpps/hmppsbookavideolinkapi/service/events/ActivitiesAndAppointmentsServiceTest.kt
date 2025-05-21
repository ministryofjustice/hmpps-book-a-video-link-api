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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Toggles
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactly
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withProbationPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NomisMappingService

private const val VLB = "VLB"
private const val VLOO = "VLOO"
private const val VLPM = "VLPM"

class ActivitiesAndAppointmentsServiceTest {
  private val activitiesAppointmentsClient: ActivitiesAppointmentsClient = mock()
  private val nomisMappingService: NomisMappingService = mock()
  private val supportedAppointmentTypes = SupportedAppointmentTypes()
  private val toggles: Toggles = mock()
  private val service = ActivitiesAndAppointmentsService(activitiesAppointmentsClient, nomisMappingService, supportedAppointmentTypes, toggles)

  private val birminghamNomisLocation = Location(locationId = 123456, locationType = VLB, "VIDEO LINK", BIRMINGHAM)
  private val courtBookingByCourt = courtBooking().withMainCourtPrisonAppointment()
  private val probationBookingByProbationTeam = probationBooking().withProbationPrisonAppointment()

  @Nested
  @DisplayName("Map court and probation appointments")
  inner class MapCourtAndProbationAppointments {

    @Test
    fun `should create VLB court appointment using appointment comments`() {
      whenever(toggles.isMasterPublicAndPrivateNotes()) doReturn false
      whenever(nomisMappingService.getNomisLocationId(courtBookingByCourt.mainHearing()!!.prisonLocationId)) doReturn birminghamNomisLocation.locationId

      service.createAppointment(courtBookingByCourt.mainHearing()!!)

      verify(nomisMappingService).getNomisLocationId(courtBookingByCourt.mainHearing()!!.prisonLocationId)
      verify(activitiesAppointmentsClient).createAppointment(
        prisonCode = BIRMINGHAM,
        prisonerNumber = courtBookingByCourt.mainHearing()!!.prisonerNumber,
        startDate = courtBookingByCourt.mainHearing()!!.appointmentDate,
        startTime = courtBookingByCourt.mainHearing()!!.startTime,
        endTime = courtBookingByCourt.mainHearing()!!.endTime,
        internalLocationId = birminghamNomisLocation.locationId,
        comments = "Court hearing comments",
        appointmentType = SupportedAppointmentTypes.Type.COURT,
      )
    }

    @Test
    fun `should create VLB court appointment using appointment prisoners notes`() {
      val courtBookingByPrison = courtBooking(createdByPrison = true).withMainCourtPrisonAppointment()

      whenever(toggles.isMasterPublicAndPrivateNotes()) doReturn true
      whenever(nomisMappingService.getNomisLocationId(birminghamLocation.id)) doReturn birminghamNomisLocation.locationId

      service.createAppointment(courtBookingByPrison.mainHearing()!!)

      verify(nomisMappingService).getNomisLocationId(birminghamLocation.id)
      verify(activitiesAppointmentsClient).createAppointment(
        prisonCode = BIRMINGHAM,
        prisonerNumber = courtBookingByPrison.mainHearing()!!.prisonerNumber,
        startDate = courtBookingByPrison.mainHearing()!!.appointmentDate,
        startTime = courtBookingByPrison.mainHearing()!!.startTime,
        endTime = courtBookingByPrison.mainHearing()!!.endTime,
        internalLocationId = birminghamNomisLocation.locationId,
        comments = "Some public prisoners notes",
        appointmentType = SupportedAppointmentTypes.Type.COURT,
      )
    }

    @Test
    fun `should create VLPM probation appointment using appointment comments`() {
      whenever(toggles.isMasterPublicAndPrivateNotes()) doReturn false
      whenever(nomisMappingService.getNomisLocationId(probationBookingByProbationTeam.probationMeeting()!!.prisonLocationId)) doReturn birminghamNomisLocation.locationId

      service.createAppointment(probationBookingByProbationTeam.probationMeeting()!!)

      verify(nomisMappingService).getNomisLocationId(probationBookingByProbationTeam.probationMeeting()!!.prisonLocationId)
      verify(activitiesAppointmentsClient).createAppointment(
        prisonCode = BIRMINGHAM,
        prisonerNumber = probationBookingByProbationTeam.probationMeeting()!!.prisonerNumber,
        startDate = probationBookingByProbationTeam.probationMeeting()!!.appointmentDate,
        startTime = probationBookingByProbationTeam.probationMeeting()!!.startTime,
        endTime = probationBookingByProbationTeam.probationMeeting()!!.endTime,
        internalLocationId = birminghamNomisLocation.locationId,
        comments = "Probation meeting comments",
        appointmentType = SupportedAppointmentTypes.Type.PROBATION,
      )
    }

    @Test
    fun `should create VLPM probation appointment using appointment prisoners notes`() {
      val probationBookingByPrison = probationBooking(createdBy = PRISON_USER_BIRMINGHAM).withProbationPrisonAppointment()

      whenever(toggles.isMasterPublicAndPrivateNotes()) doReturn true
      whenever(nomisMappingService.getNomisLocationId(probationBookingByPrison.probationMeeting()!!.prisonLocationId)) doReturn birminghamNomisLocation.locationId

      service.createAppointment(probationBookingByPrison.probationMeeting()!!)

      verify(nomisMappingService).getNomisLocationId(probationBookingByPrison.probationMeeting()!!.prisonLocationId)
      verify(activitiesAppointmentsClient).createAppointment(
        prisonCode = BIRMINGHAM,
        prisonerNumber = probationBookingByPrison.probationMeeting()!!.prisonerNumber,
        startDate = probationBookingByPrison.probationMeeting()!!.appointmentDate,
        startTime = probationBookingByPrison.probationMeeting()!!.startTime,
        endTime = probationBookingByPrison.probationMeeting()!!.endTime,
        internalLocationId = birminghamNomisLocation.locationId,
        comments = "Some public prisoners notes",
        appointmentType = SupportedAppointmentTypes.Type.PROBATION,
      )
    }
  }

  @Nested
  @DisplayName("Matching appointments")
  inner class MatchingAppointments {
    @Test
    fun `should find matching court VLB appointment`() {
      val matchingAppointment = appointmentSearchResult(courtBookingByCourt.mainHearing()!!, birminghamNomisLocation, VLB)

      whenever(
        activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
          prisonCode = courtBookingByCourt.mainHearing()!!.prisonCode(),
          prisonerNumber = courtBookingByCourt.mainHearing()!!.prisonerNumber,
          onDate = courtBookingByCourt.mainHearing()!!.appointmentDate,
          birminghamNomisLocation.locationId,
        ),
      ) doReturn listOf(matchingAppointment)

      whenever(nomisMappingService.getNomisLocationId(courtBookingByCourt.mainHearing()!!.prisonLocationId)) doReturn birminghamNomisLocation.locationId

      service.findMatchingAppointments(courtBookingByCourt.mainHearing()!!) containsExactly listOf(matchingAppointment.appointmentId)
    }

    @Test
    fun `should find matching probation VLB appointment`() {
      val matchingAppointment = appointmentSearchResult(probationBookingByProbationTeam.probationMeeting()!!, birminghamNomisLocation, VLB)

      whenever(
        activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
          prisonCode = probationBookingByProbationTeam.probationMeeting()!!.prisonCode(),
          prisonerNumber = probationBookingByProbationTeam.probationMeeting()!!.prisonerNumber,
          onDate = probationBookingByProbationTeam.probationMeeting()!!.appointmentDate,
          birminghamNomisLocation.locationId,
        ),
      ) doReturn listOf(matchingAppointment)

      whenever(nomisMappingService.getNomisLocationId(probationBookingByProbationTeam.probationMeeting()!!.prisonLocationId)) doReturn birminghamNomisLocation.locationId

      service.findMatchingAppointments(probationBookingByProbationTeam.probationMeeting()!!) containsExactly listOf(matchingAppointment.appointmentId)
    }

    @Test
    fun `should find matching VLPM appointment`() {
      val matchingAppointment = appointmentSearchResult(probationBookingByProbationTeam.probationMeeting()!!, birminghamNomisLocation, VLPM)

      whenever(
        activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
          prisonCode = probationBookingByProbationTeam.probationMeeting()!!.prisonCode(),
          prisonerNumber = probationBookingByProbationTeam.probationMeeting()!!.prisonerNumber,
          onDate = probationBookingByProbationTeam.probationMeeting()!!.appointmentDate,
          birminghamNomisLocation.locationId,
        ),
      ) doReturn listOf(matchingAppointment)

      whenever(nomisMappingService.getNomisLocationId(probationBookingByProbationTeam.probationMeeting()!!.prisonLocationId)) doReturn birminghamNomisLocation.locationId

      service.findMatchingAppointments(probationBookingByProbationTeam.probationMeeting()!!) containsExactly listOf(matchingAppointment.appointmentId)
    }

    @Test
    fun `should not find matching appointment when already cancelled`() {
      val cancelledAppointment = appointmentSearchResult(courtBookingByCourt.mainHearing()!!, birminghamNomisLocation, VLB).copy(isCancelled = true)

      whenever(
        activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
          prisonCode = courtBookingByCourt.mainHearing()!!.prisonCode(),
          prisonerNumber = courtBookingByCourt.mainHearing()!!.prisonerNumber,
          onDate = courtBookingByCourt.mainHearing()!!.appointmentDate,
          birminghamNomisLocation.locationId,
        ),
      ) doReturn listOf(cancelledAppointment)

      whenever(nomisMappingService.getNomisLocationId(courtBookingByCourt.mainHearing()!!.prisonLocationId)) doReturn birminghamNomisLocation.locationId

      service.findMatchingAppointments(courtBookingByCourt.mainHearing()!!).isEmpty() isBool true
    }

    @Test
    fun `should not find matching appointment when times do not match`() {
      val differentEndTimeAppointment = appointmentSearchResult(courtBookingByCourt.mainHearing()!!, birminghamNomisLocation, VLB).copy(endTime = courtBookingByCourt.mainHearing()!!.endTime.plusHours(1).toHourMinuteStyle())

      whenever(
        activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
          prisonCode = courtBookingByCourt.mainHearing()!!.prisonCode(),
          prisonerNumber = courtBookingByCourt.mainHearing()!!.prisonerNumber,
          onDate = courtBookingByCourt.mainHearing()!!.appointmentDate,
          birminghamNomisLocation.locationId,
        ),
      ) doReturn listOf(differentEndTimeAppointment)

      whenever(nomisMappingService.getNomisLocationId(courtBookingByCourt.mainHearing()!!.prisonLocationId)) doReturn birminghamNomisLocation.locationId

      service.findMatchingAppointments(courtBookingByCourt.mainHearing()!!).isEmpty() isBool true
    }

    @Test
    fun `should not find matching appointment when unsupported VLOO appointment type`() {
      val vlooAppointment = appointmentSearchResult(courtBookingByCourt.mainHearing()!!, birminghamNomisLocation, VLOO)

      whenever(
        activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
          prisonCode = courtBookingByCourt.mainHearing()!!.prisonCode(),
          prisonerNumber = courtBookingByCourt.mainHearing()!!.prisonerNumber,
          onDate = courtBookingByCourt.mainHearing()!!.appointmentDate,
          birminghamNomisLocation.locationId,
        ),
      ) doReturn listOf(vlooAppointment)

      whenever(nomisMappingService.getNomisLocationId(courtBookingByCourt.mainHearing()!!.prisonLocationId)) doReturn birminghamNomisLocation.locationId

      service.findMatchingAppointments(courtBookingByCourt.mainHearing()!!).isEmpty() isBool true
    }

    @Test
    fun `should not find matching VLPM appointment`() {
      val vlpmAppointment = appointmentSearchResult(probationBookingByProbationTeam.probationMeeting()!!, birminghamNomisLocation, VLPM)

      whenever(
        activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
          prisonCode = probationBookingByProbationTeam.probationMeeting()!!.prisonCode(),
          prisonerNumber = probationBookingByProbationTeam.probationMeeting()!!.prisonerNumber,
          onDate = probationBookingByProbationTeam.probationMeeting()!!.appointmentDate,
          birminghamNomisLocation.locationId,
        ),
      ) doReturn listOf(vlpmAppointment)

      whenever(nomisMappingService.getNomisLocationId(probationBookingByProbationTeam.probationMeeting()!!.prisonLocationId)) doReturn birminghamNomisLocation.locationId

      service.findMatchingAppointments(probationBookingByProbationTeam.probationMeeting()!!) containsExactly listOf(vlpmAppointment.appointmentId)
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

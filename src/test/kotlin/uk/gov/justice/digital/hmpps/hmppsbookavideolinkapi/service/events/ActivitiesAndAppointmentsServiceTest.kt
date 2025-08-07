package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactly
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withProbationPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NomisMappingService
import java.time.LocalDateTime

private const val VLB = "VLB"
private const val VLOO = "VLOO"
private const val VLPM = "VLPM"

class ActivitiesAndAppointmentsServiceTest {
  private val activitiesAppointmentsClient: ActivitiesAppointmentsClient = mock()
  private val nomisMappingService: NomisMappingService = mock()
  private val supportedAppointmentTypes = SupportedAppointmentTypes()
  private val service = ActivitiesAndAppointmentsService(activitiesAppointmentsClient, nomisMappingService, supportedAppointmentTypes)

  private val birminghamNomisLocation = Location(locationId = 123456, locationType = VLB, "VIDEO LINK", BIRMINGHAM)
  private val courtBookingByCourt = courtBooking().withMainCourtPrisonAppointment()
  private val probationBookingByProbationTeam = probationBooking().withProbationPrisonAppointment()

  @Nested
  @DisplayName("Map court and probation appointments")
  inner class MapCourtAndProbationAppointments {
    @Test
    fun `should create VLB court appointment using appointment prisoners notes`() {
      val courtBookingByPrison = courtBooking(createdByPrison = true, notesForPrisoners = "Some public prisoners notes").withMainCourtPrisonAppointment()

      service.createAppointment(courtBookingByPrison.mainHearing()!!)

      verifyNoInteractions(nomisMappingService)
      verify(activitiesAppointmentsClient).createAppointment(
        prisonCode = BIRMINGHAM,
        prisonerNumber = courtBookingByPrison.mainHearing()!!.prisonerNumber,
        startDate = courtBookingByPrison.mainHearing()!!.appointmentDate,
        startTime = courtBookingByPrison.mainHearing()!!.startTime,
        endTime = courtBookingByPrison.mainHearing()!!.endTime,
        dpsLocationId = courtBookingByPrison.mainHearing()!!.prisonLocationId,
        comments = "Some public prisoners notes",
        appointmentType = SupportedAppointmentTypes.Type.COURT,
      )
    }

    @Test
    fun `should create VLPM probation appointment using appointment prisoners notes`() {
      val probationBookingByPrison = probationBooking(createdBy = PRISON_USER_BIRMINGHAM, notesForPrisoners = "Some public prisoners notes").withProbationPrisonAppointment()

      service.createAppointment(probationBookingByPrison.probationMeeting()!!)

      verifyNoInteractions(nomisMappingService)
      verify(activitiesAppointmentsClient).createAppointment(
        prisonCode = BIRMINGHAM,
        prisonerNumber = probationBookingByPrison.probationMeeting()!!.prisonerNumber,
        startDate = probationBookingByPrison.probationMeeting()!!.appointmentDate,
        startTime = probationBookingByPrison.probationMeeting()!!.startTime,
        endTime = probationBookingByPrison.probationMeeting()!!.endTime,
        dpsLocationId = probationBookingByPrison.probationMeeting()!!.prisonLocationId,
        comments = "Some public prisoners notes",
        appointmentType = SupportedAppointmentTypes.Type.PROBATION,
      )
    }

    @Test
    fun `should patch a court appointment`() {
      val courtBooking = courtBooking().withMainCourtPrisonAppointment()

      service.patchAppointment(1, courtBooking.mainHearing()!!)

      verifyNoInteractions(nomisMappingService)

      verify(activitiesAppointmentsClient).patchAppointment(
        1,
        startDate = courtBooking.mainHearing()!!.appointmentDate,
        startTime = courtBooking.mainHearing()!!.startTime,
        endTime = courtBooking.mainHearing()!!.endTime,
        dpsLocationId = courtBooking.mainHearing()!!.prisonLocationId,
        comments = null,
      )
    }

    @Test
    fun `should patch a probation appointment`() {
      val probationBooking = probationBooking().withProbationPrisonAppointment()

      service.patchAppointment(1, probationBooking.probationMeeting()!!)

      verifyNoInteractions(nomisMappingService)

      verify(activitiesAppointmentsClient).patchAppointment(
        1,
        startDate = probationBooking.probationMeeting()!!.appointmentDate,
        startTime = probationBooking.probationMeeting()!!.startTime,
        endTime = probationBooking.probationMeeting()!!.endTime,
        dpsLocationId = probationBooking.probationMeeting()!!.prisonLocationId,
        comments = null,
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
      createdTime = LocalDateTime.now(),
    )
  }
}

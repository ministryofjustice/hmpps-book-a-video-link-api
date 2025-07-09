package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.SupportedAppointmentTypes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.PRISON_USER_BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.birminghamLocation
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactly
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withMainCourtPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.withProbationPrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NomisMappingService

private const val VLB = "VLB"
private const val VLOO = "VLOO"
private const val VLPM = "VLPM"

class PrisonServiceTest {
  private val prisonApiClient: PrisonApiClient = mock()
  private val nomisMappingService: NomisMappingService = mock()
  private val prisonerSearchClient: PrisonerSearchClient = mock()
  private val supportedAppointmentTypes = SupportedAppointmentTypes()
  private val service = PrisonService(prisonApiClient, nomisMappingService, supportedAppointmentTypes, prisonerSearchClient)

  private val birminghamNomisLocation = Location(locationId = 123456, locationType = VLB, "VIDEO LINK", BIRMINGHAM)
  private val courtBookingByCourt = courtBooking().withMainCourtPrisonAppointment()
  private val probationBookingByProbationTeam = probationBooking().withProbationPrisonAppointment()

  @Nested
  @DisplayName("Map court and probation appointments")
  inner class MapToCourtAndProbationAppointmentType {

    @Test
    fun `should create VLB court appointment using appointment prisoners notes`() {
      val courtBookingByPrison = courtBooking(createdByPrison = true, notesForPrisoners = "Some public prisoners notes").withMainCourtPrisonAppointment()

      whenever(nomisMappingService.getNomisLocationId(birminghamLocation.id)) doReturn birminghamNomisLocation.locationId
      mockPrisoner(courtBookingByPrison.mainHearing()!!)

      service.createAppointment(courtBookingByPrison.mainHearing()!!)

      verify(nomisMappingService).getNomisLocationId(birminghamLocation.id)
      verify(prisonApiClient).createAppointment(
        bookingId = 1,
        appointmentDate = courtBookingByPrison.mainHearing()!!.appointmentDate,
        startTime = courtBookingByPrison.mainHearing()!!.startTime,
        endTime = courtBookingByPrison.mainHearing()!!.endTime,
        locationId = birminghamNomisLocation.locationId,
        comments = "Some public prisoners notes",
        appointmentType = SupportedAppointmentTypes.Type.COURT,
      )
    }

    @Test
    fun `should create VLPM probation appointment using prisoners notes`() {
      val probationBookingByPrison = probationBooking(createdBy = PRISON_USER_BIRMINGHAM, notesForPrisoners = "Some public prisoners notes").withProbationPrisonAppointment()

      whenever(nomisMappingService.getNomisLocationId(probationBookingByPrison.probationMeeting()!!.prisonLocationId)) doReturn birminghamNomisLocation.locationId
      mockPrisoner(probationBookingByPrison.probationMeeting()!!)

      service.createAppointment(probationBookingByPrison.probationMeeting()!!)

      verify(nomisMappingService).getNomisLocationId(probationBookingByPrison.probationMeeting()!!.prisonLocationId)
      verify(prisonApiClient).createAppointment(
        bookingId = 1,
        appointmentDate = probationBookingByPrison.probationMeeting()!!.appointmentDate,
        startTime = probationBookingByPrison.probationMeeting()!!.startTime,
        endTime = probationBookingByPrison.probationMeeting()!!.endTime,
        locationId = birminghamNomisLocation.locationId,
        comments = "Some public prisoners notes",
        appointmentType = SupportedAppointmentTypes.Type.PROBATION,
      )
    }
  }

  @Nested
  @DisplayName("Matching appointments")
  inner class MatchingAppointments {
    @Test
    fun `should find matching VLB court appointment`() {
      val matchingAppointment = prisonerSchedule(courtBookingByCourt.mainHearing()!!, birminghamNomisLocation, VLB)

      whenever(
        prisonApiClient.getPrisonersAppointmentsAtLocations(
          prisonCode = courtBookingByCourt.mainHearing()!!.prisonCode(),
          prisonerNumber = courtBookingByCourt.mainHearing()!!.prisonerNumber,
          onDate = courtBookingByCourt.mainHearing()!!.appointmentDate,
          birminghamNomisLocation.locationId,
        ),
      ) doReturn listOf(matchingAppointment)

      whenever(nomisMappingService.getNomisLocationId(courtBookingByCourt.mainHearing()!!.prisonLocationId)) doReturn birminghamNomisLocation.locationId

      service.findMatchingAppointments(courtBookingByCourt.mainHearing()!!).containsExactly(listOf(matchingAppointment.eventId))
    }

    @Test
    fun `should find matching probation VLB appointment`() {
      val matchingAppointment = prisonerSchedule(probationBookingByProbationTeam.probationMeeting()!!, birminghamNomisLocation, VLB)

      whenever(
        prisonApiClient.getPrisonersAppointmentsAtLocations(
          prisonCode = probationBookingByProbationTeam.probationMeeting()!!.prisonCode(),
          prisonerNumber = probationBookingByProbationTeam.probationMeeting()!!.prisonerNumber,
          onDate = probationBookingByProbationTeam.probationMeeting()!!.appointmentDate,
          birminghamNomisLocation.locationId,
        ),
      ) doReturn listOf(matchingAppointment)

      whenever(nomisMappingService.getNomisLocationId(probationBookingByProbationTeam.probationMeeting()!!.prisonLocationId)) doReturn birminghamNomisLocation.locationId

      service.findMatchingAppointments(probationBookingByProbationTeam.probationMeeting()!!) containsExactly listOf(matchingAppointment.eventId)
    }

    @Test
    fun `should find matching probation VLPM appointment`() {
      val matchingAppointment = prisonerSchedule(probationBookingByProbationTeam.probationMeeting()!!, birminghamNomisLocation, VLPM)

      whenever(
        prisonApiClient.getPrisonersAppointmentsAtLocations(
          prisonCode = probationBookingByProbationTeam.probationMeeting()!!.prisonCode(),
          prisonerNumber = probationBookingByProbationTeam.probationMeeting()!!.prisonerNumber,
          onDate = probationBookingByProbationTeam.probationMeeting()!!.appointmentDate,
          birminghamNomisLocation.locationId,
        ),
      ) doReturn listOf(matchingAppointment)

      whenever(nomisMappingService.getNomisLocationId(probationBookingByProbationTeam.probationMeeting()!!.prisonLocationId)) doReturn birminghamNomisLocation.locationId

      service.findMatchingAppointments(probationBookingByProbationTeam.probationMeeting()!!) containsExactly listOf(matchingAppointment.eventId)
    }

    @Test
    fun `should not find matching appointment when times do not match`() {
      val differentEndTimeAppointment = prisonerSchedule(courtBookingByCourt.mainHearing()!!, birminghamNomisLocation, VLB).copy(endTime = courtBookingByCourt.mainHearing()!!.appointmentDate.atTime(courtBookingByCourt.mainHearing()!!.endTime.plusHours(1)))

      whenever(
        prisonApiClient.getPrisonersAppointmentsAtLocations(
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
      val vlooAppointment = prisonerSchedule(courtBookingByCourt.mainHearing()!!, birminghamNomisLocation, VLOO)

      whenever(
        prisonApiClient.getPrisonersAppointmentsAtLocations(
          prisonCode = courtBookingByCourt.mainHearing()!!.prisonCode(),
          prisonerNumber = courtBookingByCourt.mainHearing()!!.prisonerNumber,
          onDate = courtBookingByCourt.mainHearing()!!.appointmentDate,
          birminghamNomisLocation.locationId,
        ),
      ) doReturn listOf(vlooAppointment)

      whenever(nomisMappingService.getNomisLocationId(courtBookingByCourt.mainHearing()!!.prisonLocationId)) doReturn birminghamNomisLocation.locationId

      service.findMatchingAppointments(courtBookingByCourt.mainHearing()!!).isEmpty() isBool true
    }
  }

  private fun prisonerSchedule(appointment: PrisonAppointment, location: Location, appointmentType: String) = run {
    PrisonerSchedule(
      offenderNo = appointment.prisonerNumber,
      locationId = location.locationId,
      firstName = "Bob",
      lastName = "Builder",
      eventId = 99,
      event = appointmentType,
      startTime = appointment.appointmentDate.atTime(appointment.startTime),
      endTime = appointment.appointmentDate.atTime(appointment.endTime),
    )
  }

  private fun mockPrisoner(prisonAppointment: PrisonAppointment) {
    whenever(prisonerSearchClient.getPrisoner(prisonAppointment.prisonerNumber)) doReturn prisonerSearchPrisoner(
      prisonerNumber = prisonAppointment.prisonerNumber,
      prisonCode = prisonAppointment.prisonCode(),
      bookingId = 1,
    )
  }
}

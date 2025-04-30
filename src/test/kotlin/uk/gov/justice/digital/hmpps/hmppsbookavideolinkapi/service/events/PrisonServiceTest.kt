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
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.containsExactly
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.isBool
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.NomisMappingService
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

private const val VLB = "VLB"
private const val VLOO = "VLOO"
private const val VLPM = "VLPM"

class PrisonServiceTest {
  private val prisonApiClient: PrisonApiClient = mock()
  private val nomisMappingService: NomisMappingService = mock()
  private val prisonerSearchClient: PrisonerSearchClient = mock()
  private val supportedAppointmentTypes = SupportedAppointmentTypes()
  private val service = PrisonService(prisonApiClient, nomisMappingService, supportedAppointmentTypes, prisonerSearchClient)

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
  @DisplayName("Map correct appointment type for bookings")
  inner class MapToCourtAndProbationAppointmentType {

    @Test
    fun `should create VLB court appointment`() {
      whenever(nomisMappingService.getNomisLocationId(courtAppointment.prisonLocationId)) doReturn birminghamLocation.locationId
      whenever(prisonerSearchClient.getPrisoner(courtAppointment.prisonerNumber)) doReturn prisonerSearchPrisoner(
        prisonerNumber = courtAppointment.prisonerNumber,
        prisonCode = courtAppointment.prisonCode(),
        bookingId = 1,
      )

      service.createAppointment(courtAppointment)

      verify(nomisMappingService).getNomisLocationId(courtAppointment.prisonLocationId)
      verify(prisonApiClient).createAppointment(
        bookingId = 1,
        appointmentDate = LocalDate.of(2100, 1, 1),
        startTime = LocalTime.of(11, 0),
        endTime = LocalTime.of(11, 30),
        locationId = 123456,
        comments = "Court hearing comments",
        appointmentType = SupportedAppointmentTypes.Type.COURT,
      )
    }

    @Test
    fun `should create VLPM probation appointment`() {
      whenever(nomisMappingService.getNomisLocationId(probationAppointment.prisonLocationId)) doReturn birminghamLocation.locationId
      whenever(prisonerSearchClient.getPrisoner(probationAppointment.prisonerNumber)) doReturn prisonerSearchPrisoner(
        prisonerNumber = probationAppointment.prisonerNumber,
        prisonCode = probationAppointment.prisonCode(),
        bookingId = 1,
      )

      service.createAppointment(probationAppointment)

      verify(nomisMappingService).getNomisLocationId(probationAppointment.prisonLocationId)
      verify(prisonApiClient).createAppointment(
        bookingId = 1,
        appointmentDate = LocalDate.of(2100, 1, 1),
        startTime = LocalTime.of(11, 0),
        endTime = LocalTime.of(11, 30),
        locationId = 123456,
        comments = "Probation meeting comments",
        appointmentType = SupportedAppointmentTypes.Type.PROBATION,
      )
    }
  }

  @Nested
  @DisplayName("Matching appointments")
  inner class MatchingAppointments {
    @Test
    fun `should find matching VLB court appointment`() {
      val matchingAppointment = prisonerSchedule(courtAppointment, birminghamLocation, VLB)

      whenever(
        prisonApiClient.getPrisonersAppointmentsAtLocations(
          prisonCode = courtAppointment.prisonCode(),
          prisonerNumber = courtAppointment.prisonerNumber,
          onDate = courtAppointment.appointmentDate,
          birminghamLocation.locationId,
        ),
      ) doReturn listOf(matchingAppointment)

      whenever(nomisMappingService.getNomisLocationId(courtAppointment.prisonLocationId)) doReturn birminghamLocation.locationId

      service.findMatchingAppointments(courtAppointment).containsExactly(listOf(matchingAppointment.eventId))
    }

    @Test
    fun `should find matching probation VLB appointment`() {
      val matchingAppointment = prisonerSchedule(probationAppointment, birminghamLocation, VLB)

      whenever(
        prisonApiClient.getPrisonersAppointmentsAtLocations(
          prisonCode = probationAppointment.prisonCode(),
          prisonerNumber = probationAppointment.prisonerNumber,
          onDate = probationAppointment.appointmentDate,
          birminghamLocation.locationId,
        ),
      ) doReturn listOf(matchingAppointment)

      whenever(nomisMappingService.getNomisLocationId(probationAppointment.prisonLocationId)) doReturn birminghamLocation.locationId

      service.findMatchingAppointments(probationAppointment) containsExactly listOf(matchingAppointment.eventId)
    }

    @Test
    fun `should find matching probation VLPM appointment`() {
      val matchingAppointment = prisonerSchedule(probationAppointment, birminghamLocation, VLPM)

      whenever(
        prisonApiClient.getPrisonersAppointmentsAtLocations(
          prisonCode = probationAppointment.prisonCode(),
          prisonerNumber = probationAppointment.prisonerNumber,
          onDate = probationAppointment.appointmentDate,
          birminghamLocation.locationId,
        ),
      ) doReturn listOf(matchingAppointment)

      whenever(nomisMappingService.getNomisLocationId(probationAppointment.prisonLocationId)) doReturn birminghamLocation.locationId

      service.findMatchingAppointments(probationAppointment) containsExactly listOf(matchingAppointment.eventId)
    }

    @Test
    fun `should not find matching appointment when times do not match`() {
      val differentEndTimeAppointment = prisonerSchedule(courtAppointment, birminghamLocation, VLB).copy(endTime = courtAppointment.appointmentDate.atTime(courtAppointment.endTime.plusHours(1)))

      whenever(
        prisonApiClient.getPrisonersAppointmentsAtLocations(
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
      val vlooAppointment = prisonerSchedule(courtAppointment, birminghamLocation, VLOO)

      whenever(
        prisonApiClient.getPrisonersAppointmentsAtLocations(
          prisonCode = courtAppointment.prisonCode(),
          prisonerNumber = courtAppointment.prisonerNumber,
          onDate = courtAppointment.appointmentDate,
          birminghamLocation.locationId,
        ),
      ) doReturn listOf(vlooAppointment)

      whenever(nomisMappingService.getNomisLocationId(courtAppointment.prisonLocationId)) doReturn birminghamLocation.locationId

      service.findMatchingAppointments(probationAppointment).isEmpty() isBool true
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
}

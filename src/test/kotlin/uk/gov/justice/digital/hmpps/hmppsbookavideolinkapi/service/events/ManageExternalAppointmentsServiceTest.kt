package uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.service.events

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.ActivitiesAppointmentsClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentAttendeeSearchResult
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentCategorySummary
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentLocationSummary
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.activitiesappointments.model.AppointmentSearchResult
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.nomismapping.NomisDpsLocationMapping
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.nomismapping.NomisMappingClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonApiClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.PrisonerSchedule
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonapi.model.Location
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.client.prisonersearch.PrisonerSearchClient
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.SupportedAppointmentTypes
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.common.toHourMinuteStyle
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.Feature
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.config.FeatureSwitches
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistory
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.BookingHistoryAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.HistoryType
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.PrisonAppointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.entity.VideoBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.BIRMINGHAM
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.WANDSWORTH
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.appointment
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.courtBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.prisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.helper.probationBooking
import uk.gov.justice.digital.hmpps.hmppsbookavideolinkapi.repository.PrisonAppointmentRepository
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional
import java.util.UUID

class ManageExternalAppointmentsServiceTest {
  private val prisonAppointmentRepository: PrisonAppointmentRepository = mock()
  private val activitiesAppointmentsClient: ActivitiesAppointmentsClient = mock()
  private val prisonApiClient: PrisonApiClient = mock()
  private val prisonerSearchClient: PrisonerSearchClient = mock()
  private val nomisMappingClient: NomisMappingClient = mock()
  private val featureSwitches: FeatureSwitches = mock()
  private val supportedAppointmentTypes = SupportedAppointmentTypes(featureSwitches)
  private val birminghamLocation = Location(
    locationId = 123456,
    locationType = "VLB",
    "VIDEO LINK",
    BIRMINGHAM,
  )
  private val wandsworthLocation = birminghamLocation.copy(agencyId = WANDSWORTH, locationId = 654321)
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
    prisonCode = WANDSWORTH,
    prisonerNumber = "654321",
    appointmentType = "VLB_PROBATION",
    date = LocalDate.of(2100, 1, 1),
    startTime = LocalTime.of(11, 0),
    endTime = LocalTime.of(11, 30),
    locationId = UUID.randomUUID(),
  )
  private val service =
    ManageExternalAppointmentsService(
      prisonAppointmentRepository,
      activitiesAppointmentsClient,
      prisonApiClient,
      prisonerSearchClient,
      nomisMappingClient,
      supportedAppointmentTypes,
    )

  @Nested
  @DisplayName("Map correct appointment type for bookings when probation feature is not live")
  inner class MapCourtAppointmentType {
    @BeforeEach
    fun before() {
      whenever(featureSwitches.isEnabled(Feature.FEATURE_MASTER_VLPM_TYPES)) doReturn false
    }

    @Test
    fun `should create court appointment via activities client when appointments rolled out`() {
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
      whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(BIRMINGHAM)) doReturn true
      whenever(nomisMappingClient.getNomisLocationMappingBy(courtAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = birminghamLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)

      service.createAppointment(1)

      verify(nomisMappingClient).getNomisLocationMappingBy(courtAppointment.prisonLocationId)

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
    fun `should create court appointment without comments via activities client when appointments rolled out`() {
      val courtAppointmentWithoutComments = appointment(
        booking = courtBooking.apply { comments = null },
        prisonCode = BIRMINGHAM,
        prisonerNumber = "123456",
        appointmentType = "VLB_COURT_PRE",
        date = LocalDate.of(2100, 1, 1),
        startTime = LocalTime.of(11, 0),
        endTime = LocalTime.of(11, 30),
        locationId = UUID.randomUUID(),
      )

      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointmentWithoutComments)
      whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(BIRMINGHAM)) doReturn true
      whenever(nomisMappingClient.getNomisLocationMappingBy(courtAppointmentWithoutComments.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = birminghamLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)

      service.createAppointment(1)

      verify(nomisMappingClient).getNomisLocationMappingBy(courtAppointmentWithoutComments.prisonLocationId)

      verify(activitiesAppointmentsClient).createAppointment(
        prisonCode = BIRMINGHAM,
        prisonerNumber = "123456",
        startDate = LocalDate.of(2100, 1, 1),
        startTime = LocalTime.of(11, 0),
        endTime = LocalTime.of(11, 30),
        internalLocationId = 123456,
        comments = null,
        appointmentType = SupportedAppointmentTypes.Type.COURT,
      )
    }

    @Test
    fun `should not create court appointment via activities client when appointment already exists`() {
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
      whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(BIRMINGHAM)) doReturn true
      whenever(nomisMappingClient.getNomisLocationMappingBy(courtAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = birminghamLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)
      whenever(
        activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
          prisonCode = courtAppointment.prisonCode(),
          prisonerNumber = courtAppointment.prisonerNumber,
          onDate = courtAppointment.appointmentDate,
          birminghamLocation.locationId,
        ),
      ) doReturn listOf(appointmentSearchResult(courtAppointment, birminghamLocation, "VLB"))

      service.createAppointment(1)

      inOrder(nomisMappingClient, activitiesAppointmentsClient) {
        verify(nomisMappingClient).getNomisLocationMappingBy(courtAppointment.prisonLocationId)
        verify(activitiesAppointmentsClient).getPrisonersAppointmentsAtLocations(
          prisonCode = courtAppointment.prisonCode(),
          prisonerNumber = courtAppointment.prisonerNumber,
          onDate = courtAppointment.appointmentDate,
          birminghamLocation.locationId,
        )
      }

      verify(activitiesAppointmentsClient, never()).createAppointment(any(), any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `should create court appointment via activities client when a cancelled appointment already exists`() {
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
      whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(BIRMINGHAM)) doReturn true
      whenever(nomisMappingClient.getNomisLocationMappingBy(courtAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = birminghamLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)
      whenever(
        activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
          prisonCode = courtAppointment.prisonCode(),
          prisonerNumber = courtAppointment.prisonerNumber,
          onDate = courtAppointment.appointmentDate,
          birminghamLocation.locationId,
        ),
      ) doReturn listOf(appointmentSearchResult(courtAppointment, birminghamLocation, "VLB").copy(isCancelled = true))

      service.createAppointment(1)

      inOrder(nomisMappingClient, activitiesAppointmentsClient) {
        verify(nomisMappingClient).getNomisLocationMappingBy(courtAppointment.prisonLocationId)
        verify(activitiesAppointmentsClient).getPrisonersAppointmentsAtLocations(
          prisonCode = courtAppointment.prisonCode(),
          prisonerNumber = courtAppointment.prisonerNumber,
          onDate = courtAppointment.appointmentDate,
          birminghamLocation.locationId,
        )
      }

      verify(activitiesAppointmentsClient).createAppointment(any(), any(), any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `should create court appointment via prison api client when appointments not rolled out`() {
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
      whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(BIRMINGHAM)) doReturn false
      whenever(prisonerSearchClient.getPrisoner(courtAppointment.prisonerNumber)) doReturn prisonerSearchPrisoner(
        prisonerNumber = courtAppointment.prisonerNumber,
        prisonCode = courtAppointment.prisonCode(),
        bookingId = 1,
      )
      whenever(nomisMappingClient.getNomisLocationMappingBy(courtAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = birminghamLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)

      service.createAppointment(1)

      verify(activitiesAppointmentsClient, never()).createAppointment(any(), any(), any(), any(), any(), any(), any(), any())
      verify(prisonApiClient).createAppointment(
        bookingId = 1,
        locationId = 123456,
        appointmentDate = LocalDate.of(2100, 1, 1),
        startTime = LocalTime.of(11, 0),
        endTime = LocalTime.of(11, 30),
        comments = "Court hearing comments",
        appointmentType = SupportedAppointmentTypes.Type.COURT,
      )
    }

    @Test
    fun `should create court appointment without comments via prison api client when appointments not rolled out`() {
      val courtAppointmentWithoutComments = appointment(
        booking = courtBooking.apply { comments = null },
        prisonCode = BIRMINGHAM,
        prisonerNumber = "123456",
        appointmentType = "VLB_COURT_PRE",
        date = LocalDate.of(2100, 1, 1),
        startTime = LocalTime.of(11, 0),
        endTime = LocalTime.of(11, 30),
        locationId = UUID.randomUUID(),
      )

      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointmentWithoutComments)
      whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(BIRMINGHAM)) doReturn false
      whenever(prisonerSearchClient.getPrisoner(courtAppointment.prisonerNumber)) doReturn prisonerSearchPrisoner(
        prisonerNumber = courtAppointment.prisonerNumber,
        prisonCode = courtAppointment.prisonCode(),
        bookingId = 1,
      )
      whenever(nomisMappingClient.getNomisLocationMappingBy(courtAppointmentWithoutComments.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = birminghamLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)

      service.createAppointment(1)

      verify(activitiesAppointmentsClient, never()).createAppointment(any(), any(), any(), any(), any(), any(), any(), any())
      verify(prisonApiClient).createAppointment(
        bookingId = 1,
        locationId = 123456,
        appointmentDate = LocalDate.of(2100, 1, 1),
        startTime = LocalTime.of(11, 0),
        endTime = LocalTime.of(11, 30),
        comments = null,
        appointmentType = SupportedAppointmentTypes.Type.COURT,
      )
    }

    @Test
    fun `should create probation appointment via prison api client when appointments not rolled out`() {
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(probationAppointment)
      whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(WANDSWORTH)) doReturn false
      whenever(prisonerSearchClient.getPrisoner(probationAppointment.prisonerNumber)) doReturn prisonerSearchPrisoner(
        prisonerNumber = probationAppointment.prisonerNumber,
        prisonCode = probationAppointment.prisonCode(),
        bookingId = 1,
      )
      whenever(nomisMappingClient.getNomisLocationMappingBy(probationAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = wandsworthLocation.locationId, dpsLocationId = probationAppointment.prisonLocationId)

      service.createAppointment(1)

      verify(activitiesAppointmentsClient, never()).createAppointment(any(), any(), any(), any(), any(), any(), any(), any())
      verify(prisonApiClient).createAppointment(
        bookingId = 1,
        locationId = wandsworthLocation.locationId,
        appointmentDate = LocalDate.of(2100, 1, 1),
        startTime = LocalTime.of(11, 0),
        endTime = LocalTime.of(11, 30),
        comments = "Probation meeting comments",
        appointmentType = SupportedAppointmentTypes.Type.COURT,
      )
    }

    @Test
    fun `should create probation appointment via activities api client when appointments rolled out`() {
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(probationAppointment)
      whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(WANDSWORTH)) doReturn false
      whenever(prisonerSearchClient.getPrisoner(probationAppointment.prisonerNumber)) doReturn prisonerSearchPrisoner(
        prisonerNumber = probationAppointment.prisonerNumber,
        prisonCode = probationAppointment.prisonCode(),
        bookingId = 1,
      )
      whenever(nomisMappingClient.getNomisLocationMappingBy(probationAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = wandsworthLocation.locationId, dpsLocationId = probationAppointment.prisonLocationId)

      service.createAppointment(1)

      verify(activitiesAppointmentsClient, never()).createAppointment(any(), any(), any(), any(), any(), any(), any(), any())
      verify(prisonApiClient).createAppointment(
        bookingId = 1,
        locationId = wandsworthLocation.locationId,
        appointmentDate = LocalDate.of(2100, 1, 1),
        startTime = LocalTime.of(11, 0),
        endTime = LocalTime.of(11, 30),
        comments = "Probation meeting comments",
        appointmentType = SupportedAppointmentTypes.Type.COURT,
      )
    }
  }

  @Nested
  @DisplayName("Map correct appointment type for bookings when probation feature is live")
  inner class MapCourtAndProbationAppointmentType {
    @BeforeEach
    fun before() {
      whenever(featureSwitches.isEnabled(Feature.FEATURE_MASTER_VLPM_TYPES)) doReturn true
    }

    @Test
    fun `should create court appointment via activities client when appointments rolled out`() {
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
      whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(BIRMINGHAM)) doReturn true
      whenever(nomisMappingClient.getNomisLocationMappingBy(courtAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = birminghamLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)

      service.createAppointment(1)

      verify(nomisMappingClient).getNomisLocationMappingBy(courtAppointment.prisonLocationId)

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
    fun `should create court appointment via prison api client when appointments not rolled out`() {
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
      whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(BIRMINGHAM)) doReturn false
      whenever(prisonerSearchClient.getPrisoner(courtAppointment.prisonerNumber)) doReturn prisonerSearchPrisoner(
        prisonerNumber = courtAppointment.prisonerNumber,
        prisonCode = courtAppointment.prisonCode(),
        bookingId = 1,
      )
      whenever(nomisMappingClient.getNomisLocationMappingBy(courtAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = birminghamLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)

      service.createAppointment(1)

      verify(activitiesAppointmentsClient, never()).createAppointment(any(), any(), any(), any(), any(), any(), any(), any())
      verify(prisonApiClient).createAppointment(
        bookingId = 1,
        locationId = 123456,
        appointmentDate = LocalDate.of(2100, 1, 1),
        startTime = LocalTime.of(11, 0),
        endTime = LocalTime.of(11, 30),
        comments = "Court hearing comments",
        appointmentType = SupportedAppointmentTypes.Type.COURT,
      )
    }

    @Test
    fun `should create probation appointment via prison api client when appointments not rolled out`() {
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(probationAppointment)
      whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(WANDSWORTH)) doReturn false
      whenever(prisonerSearchClient.getPrisoner(probationAppointment.prisonerNumber)) doReturn prisonerSearchPrisoner(
        prisonerNumber = probationAppointment.prisonerNumber,
        prisonCode = probationAppointment.prisonCode(),
        bookingId = 1,
      )
      whenever(nomisMappingClient.getNomisLocationMappingBy(probationAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = wandsworthLocation.locationId, dpsLocationId = probationAppointment.prisonLocationId)

      service.createAppointment(1)

      verify(activitiesAppointmentsClient, never()).createAppointment(any(), any(), any(), any(), any(), any(), any(), any())
      verify(prisonApiClient).createAppointment(
        bookingId = 1,
        locationId = wandsworthLocation.locationId,
        appointmentDate = LocalDate.of(2100, 1, 1),
        startTime = LocalTime.of(11, 0),
        endTime = LocalTime.of(11, 30),
        comments = "Probation meeting comments",
        appointmentType = SupportedAppointmentTypes.Type.PROBATION,
      )
    }

    @Test
    fun `should create probation appointment via activities api client when appointments rolled out`() {
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(probationAppointment)
      whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(WANDSWORTH)) doReturn false
      whenever(prisonerSearchClient.getPrisoner(probationAppointment.prisonerNumber)) doReturn prisonerSearchPrisoner(
        prisonerNumber = probationAppointment.prisonerNumber,
        prisonCode = probationAppointment.prisonCode(),
        bookingId = 1,
      )
      whenever(nomisMappingClient.getNomisLocationMappingBy(probationAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = wandsworthLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)

      service.createAppointment(1)

      verify(activitiesAppointmentsClient, never()).createAppointment(any(), any(), any(), any(), any(), any(), any(), any())
      verify(prisonApiClient).createAppointment(
        bookingId = 1,
        locationId = wandsworthLocation.locationId,
        appointmentDate = LocalDate.of(2100, 1, 1),
        startTime = LocalTime.of(11, 0),
        endTime = LocalTime.of(11, 30),
        comments = "Probation meeting comments",
        appointmentType = SupportedAppointmentTypes.Type.PROBATION,
      )
    }
  }

  @Test
  fun `should not create appointment via prison api client when appointment already exists`() {
    whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
    whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(BIRMINGHAM)) doReturn false
    whenever(prisonerSearchClient.getPrisoner(courtAppointment.prisonerNumber)) doReturn prisonerSearchPrisoner(
      prisonerNumber = courtAppointment.prisonerNumber,
      prisonCode = courtAppointment.prisonCode(),
      bookingId = 1,
    )
    whenever(nomisMappingClient.getNomisLocationMappingBy(courtAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = birminghamLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)
    whenever(
      prisonApiClient.getPrisonersAppointmentsAtLocations(
        courtAppointment.prisonCode(),
        courtAppointment.prisonerNumber,
        courtAppointment.appointmentDate,
        birminghamLocation.locationId,
      ),
    ) doReturn listOf(prisonerSchedule(courtAppointment, birminghamLocation, "VLB"))

    service.createAppointment(1)

    inOrder(nomisMappingClient, prisonApiClient) {
      verify(nomisMappingClient).getNomisLocationMappingBy(courtAppointment.prisonLocationId)
      verify(prisonApiClient).getPrisonersAppointmentsAtLocations(
        prisonCode = courtAppointment.prisonCode(),
        prisonerNumber = courtAppointment.prisonerNumber,
        onDate = courtAppointment.appointmentDate,
        birminghamLocation.locationId,
      )
    }

    verify(prisonApiClient, never()).createAppointment(any(), any(), any(), any(), any(), any(), any())
  }

  @Test
  fun `should be no-op when appointment not found`() {
    whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.empty()

    service.createAppointment(1)

    verifyNoInteractions(activitiesAppointmentsClient)
    verifyNoInteractions(prisonApiClient)
  }

  @Nested
  @DisplayName("Cancel appointment")
  inner class CancelAppointment {

    @Test
    fun `should cancel court appointment via activities client when appointments rolled out`() {
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
      whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(courtAppointment.prisonCode())) doReturn true
      whenever(nomisMappingClient.getNomisLocationMappingBy(courtAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = birminghamLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)
      whenever(
        activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
          courtAppointment.prisonCode(),
          courtAppointment.prisonerNumber,
          courtAppointment.appointmentDate,
          birminghamLocation.locationId,
        ),
      ) doReturn listOf(appointmentSearchResult(courtAppointment, birminghamLocation, "VLB"))

      service.cancelCurrentAppointment(1)

      verify(activitiesAppointmentsClient).cancelAppointment(99)
    }

    @Test
    fun `should cancel VLB probation booking via activities client when appointments rolled out`() {
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(probationAppointment)
      whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(probationAppointment.prisonCode())) doReturn true
      whenever(nomisMappingClient.getNomisLocationMappingBy(probationAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = wandsworthLocation.locationId, dpsLocationId = probationAppointment.prisonLocationId)
      whenever(
        activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
          probationAppointment.prisonCode(),
          probationAppointment.prisonerNumber,
          probationAppointment.appointmentDate,
          wandsworthLocation.locationId,
        ),
      ) doReturn listOf(appointmentSearchResult(probationAppointment, wandsworthLocation, "VLB"))

      service.cancelCurrentAppointment(1)

      verify(activitiesAppointmentsClient).cancelAppointment(99)
    }

    @Test
    fun `should cancel VLPM probation booking via activities client when appointments rolled out`() {
      whenever(featureSwitches.isEnabled(Feature.FEATURE_MASTER_VLPM_TYPES)) doReturn true
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(probationAppointment)
      whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(probationAppointment.prisonCode())) doReturn true
      whenever(nomisMappingClient.getNomisLocationMappingBy(probationAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = wandsworthLocation.locationId, dpsLocationId = probationAppointment.prisonLocationId)
      whenever(
        activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
          probationAppointment.prisonCode(),
          probationAppointment.prisonerNumber,
          probationAppointment.appointmentDate,
          wandsworthLocation.locationId,
        ),
      ) doReturn listOf(appointmentSearchResult(probationAppointment, wandsworthLocation, "VLPM"))

      service.cancelCurrentAppointment(1)

      verify(activitiesAppointmentsClient).cancelAppointment(99)
    }

    @Test
    fun `should cancel previous VLPM probation appointment via activities client when appointments rolled out`() {
      val bookingHistory = buildFakeBookingHistory(probationBooking, probationAppointment)

      whenever(featureSwitches.isEnabled(Feature.FEATURE_MASTER_VLPM_TYPES)) doReturn true
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(probationAppointment)
      whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(probationAppointment.prisonCode())) doReturn true
      whenever(nomisMappingClient.getNomisLocationMappingBy(probationAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = wandsworthLocation.locationId, dpsLocationId = probationAppointment.prisonLocationId)
      whenever(
        activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
          probationAppointment.prisonCode(),
          probationAppointment.prisonerNumber,
          probationAppointment.appointmentDate,
          wandsworthLocation.locationId,
        ),
      ) doReturn listOf(appointmentSearchResult(probationAppointment, wandsworthLocation, "VLPM"))

      service.cancelPreviousAppointment(bookingHistory.appointments().single())

      verify(activitiesAppointmentsClient).cancelAppointment(99, deleteOnCancel = true)
    }

    @Test
    fun `should ignore cancel appointment via activities client when appointment already cancelled`() {
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
      whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(courtAppointment.prisonCode())) doReturn true
      whenever(nomisMappingClient.getNomisLocationMappingBy(courtAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = birminghamLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)
      whenever(
        activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
          courtAppointment.prisonCode(),
          courtAppointment.prisonerNumber,
          courtAppointment.appointmentDate,
          birminghamLocation.locationId,
        ),
      ) doReturn listOf(appointmentSearchResult(courtAppointment, birminghamLocation, "VLB").copy(isCancelled = true))

      service.cancelCurrentAppointment(1)

      verify(activitiesAppointmentsClient, never()).cancelAppointment(any(), any())
    }

    @Test
    fun `should not cancel appointment via activities client when appointments rolled out but matching appointment not found`() {
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
      whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(courtAppointment.prisonCode())) doReturn true
      whenever(nomisMappingClient.getNomisLocationMappingBy(courtAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = birminghamLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)
      whenever(
        activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
          courtAppointment.prisonCode(),
          courtAppointment.prisonerNumber,
          courtAppointment.appointmentDate,
          birminghamLocation.locationId,
        ),
      ) doReturn listOf(
        AppointmentSearchResult(
          appointmentType = AppointmentSearchResult.AppointmentType.INDIVIDUAL,
          startDate = courtAppointment.appointmentDate,
          startTime = courtAppointment.startTime.plusMinutes(1).toHourMinuteStyle(),
          endTime = courtAppointment.endTime.toHourMinuteStyle(),
          isCancelled = false,
          isExpired = false,
          isEdited = false,
          appointmentId = 99,
          appointmentSeriesId = 1,
          appointmentName = "appointment name",
          // TIME DOES NOT MATCH
          attendees = listOf(AppointmentAttendeeSearchResult(1, courtAppointment.prisonerNumber, 1)),
          category = AppointmentCategorySummary("VLB", "video link booking"),
          inCell = false,
          isRepeat = false,
          maxSequenceNumber = 1,
          prisonCode = courtAppointment.prisonCode(),
          sequenceNumber = 1,
          internalLocation = AppointmentLocationSummary(
            birminghamLocation.locationId,
            courtAppointment.prisonCode(),
            "VIDEO LINK",
          ),
          timeSlot = AppointmentSearchResult.TimeSlot.AM,
        ),
      )

      service.cancelCurrentAppointment(1)

      verify(activitiesAppointmentsClient, never()).cancelAppointment(anyLong(), any())
    }

    @Test
    fun `should cancel court appointment via prison api client when appointments not rolled out`() {
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
      whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(courtAppointment.prisonCode())) doReturn false
      whenever(nomisMappingClient.getNomisLocationMappingBy(courtAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = birminghamLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)
      whenever(
        prisonApiClient.getPrisonersAppointmentsAtLocations(
          courtAppointment.prisonCode(),
          courtAppointment.prisonerNumber,
          courtAppointment.appointmentDate,
          birminghamLocation.locationId,
        ),
      ) doReturn listOf(prisonerSchedule(courtAppointment, birminghamLocation, "VLB"))

      service.cancelCurrentAppointment(1)

      verify(prisonApiClient).cancelAppointment(99)
    }

    @Test
    fun `should cancel VLB probation appointment via prison api client when appointments not rolled out`() {
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(probationAppointment)
      whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(probationAppointment.prisonCode())) doReturn false
      whenever(nomisMappingClient.getNomisLocationMappingBy(probationAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = wandsworthLocation.locationId, dpsLocationId = probationAppointment.prisonLocationId)
      whenever(
        prisonApiClient.getPrisonersAppointmentsAtLocations(
          probationAppointment.prisonCode(),
          probationAppointment.prisonerNumber,
          probationAppointment.appointmentDate,
          wandsworthLocation.locationId,
        ),
      ) doReturn listOf(prisonerSchedule(probationAppointment, wandsworthLocation, "VLB"))

      service.cancelCurrentAppointment(1)

      verify(prisonApiClient).cancelAppointment(99)
    }

    @Test
    fun `should cancel VLPM probation appointment via prison api client when appointments not rolled out`() {
      whenever(featureSwitches.isEnabled(Feature.FEATURE_MASTER_VLPM_TYPES)) doReturn true
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(probationAppointment)
      whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(probationAppointment.prisonCode())) doReturn false
      whenever(nomisMappingClient.getNomisLocationMappingBy(probationAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = wandsworthLocation.locationId, dpsLocationId = probationAppointment.prisonLocationId)
      whenever(
        prisonApiClient.getPrisonersAppointmentsAtLocations(
          probationAppointment.prisonCode(),
          probationAppointment.prisonerNumber,
          probationAppointment.appointmentDate,
          wandsworthLocation.locationId,
        ),
      ) doReturn listOf(prisonerSchedule(probationAppointment, wandsworthLocation, "VLPM"))

      service.cancelCurrentAppointment(1)

      verify(prisonApiClient).cancelAppointment(99)
    }

    @Test
    fun `should cancel previous VLPM probation appointment via prison api client when appointments not rolled out`() {
      val bookingHistory = buildFakeBookingHistory(probationBooking, probationAppointment)
      whenever(featureSwitches.isEnabled(Feature.FEATURE_MASTER_VLPM_TYPES)) doReturn true
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(probationAppointment)
      whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(probationAppointment.prisonCode())) doReturn false
      whenever(nomisMappingClient.getNomisLocationMappingBy(probationAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = wandsworthLocation.locationId, dpsLocationId = probationAppointment.prisonLocationId)
      whenever(
        prisonApiClient.getPrisonersAppointmentsAtLocations(
          probationAppointment.prisonCode(),
          probationAppointment.prisonerNumber,
          probationAppointment.appointmentDate,
          wandsworthLocation.locationId,
        ),
      ) doReturn listOf(prisonerSchedule(probationAppointment, wandsworthLocation, "VLPM"))

      service.cancelPreviousAppointment(bookingHistory.appointments().single())

      verify(prisonApiClient).cancelAppointment(99)
    }

    @Test
    fun `should not cancel appointment via prison api client when appointments not rolled out but matching appointment not found`() {
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
      whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(courtAppointment.prisonCode())) doReturn false
      whenever(nomisMappingClient.getNomisLocationMappingBy(courtAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = birminghamLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)
      whenever(
        prisonApiClient.getPrisonersAppointmentsAtLocations(
          courtAppointment.prisonCode(),
          courtAppointment.prisonerNumber,
          courtAppointment.appointmentDate,
          birminghamLocation.locationId,
        ),
      ) doReturn listOf(
        PrisonerSchedule(
          offenderNo = courtAppointment.prisonerNumber,
          locationId = 99,
          firstName = "Bob",
          lastName = "Builder",
          eventId = 99,
          event = "VLB",
          // TIME DOES NOT MATCH
          startTime = courtAppointment.appointmentDate.atTime(courtAppointment.startTime.plusMinutes(1)),
          endTime = courtAppointment.appointmentDate.atTime(courtAppointment.endTime),
        ),
      )

      service.cancelCurrentAppointment(1)

      verify(prisonApiClient, never()).cancelAppointment(anyLong())
    }

    @Test
    fun `should not cancel appointment appointment not found`() {
      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.empty()

      service.cancelCurrentAppointment(1)

      verifyNoInteractions(activitiesAppointmentsClient)
      verifyNoInteractions(prisonApiClient)
    }

    @Test
    fun `should cancel previous appointment via A&A API when A&A is rolled out`() {
      val bookingHistory = buildFakeBookingHistory(courtBooking, courtAppointment)

      whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(courtAppointment.prisonCode())) doReturn true

      whenever(
        activitiesAppointmentsClient.getPrisonersAppointmentsAtLocations(
          courtAppointment.prisonCode(),
          courtAppointment.prisonerNumber,
          courtAppointment.appointmentDate,
          birminghamLocation.locationId,
        ),
      ) doReturn listOf(appointmentSearchResult(courtAppointment, birminghamLocation, "VLB"))

      whenever(nomisMappingClient.getNomisLocationMappingBy(courtAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = birminghamLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)

      service.cancelPreviousAppointment(bookingHistory.appointments().first())

      verify(activitiesAppointmentsClient).cancelAppointment(99, true)
      verify(prisonApiClient, times(0)).cancelAppointment(anyLong())
    }

    @Test
    fun `should cancel previous appointment via Prison API when A&A is not rolled out`() {
      val bookingHistory = buildFakeBookingHistory(courtBooking, courtAppointment)

      whenever(activitiesAppointmentsClient.isAppointmentsRolledOutAt(courtAppointment.prisonCode())) doReturn false

      whenever(prisonAppointmentRepository.findById(1)) doReturn Optional.of(courtAppointment)
      whenever(nomisMappingClient.getNomisLocationMappingBy(courtAppointment.prisonLocationId)) doReturn NomisDpsLocationMapping(nomisLocationId = birminghamLocation.locationId, dpsLocationId = courtAppointment.prisonLocationId)
      whenever(
        prisonApiClient.getPrisonersAppointmentsAtLocations(
          courtAppointment.prisonCode(),
          courtAppointment.prisonerNumber,
          courtAppointment.appointmentDate,
          birminghamLocation.locationId,
        ),
      ) doReturn listOf(prisonerSchedule(courtAppointment, birminghamLocation, "VLB"))

      service.cancelPreviousAppointment(bookingHistory.appointments().first())

      verify(activitiesAppointmentsClient, times(0)).cancelAppointment(anyLong(), any())
      verify(prisonApiClient).cancelAppointment(99)
    }
  }

  private fun buildFakeBookingHistory(booking: VideoBooking, prisonAppointment: PrisonAppointment): BookingHistory {
    val bookingHistory = BookingHistory(
      bookingHistoryId = 1L,
      videoBookingId = booking.videoBookingId,
      historyType = HistoryType.CREATE,
      courtId = booking.court?.courtId,
      hearingType = booking.hearingType,
      createdBy = booking.createdBy,
    )

    val bookingHistoryAppointment = BookingHistoryAppointment(
      bookingHistoryAppointmentId = 1L,
      prisonCode = prisonAppointment.prisonCode(),
      prisonerNumber = prisonAppointment.prisonerNumber,
      appointmentDate = prisonAppointment.appointmentDate,
      appointmentType = prisonAppointment.appointmentType,
      prisonLocationId = prisonAppointment.prisonLocationId,
      startTime = prisonAppointment.startTime,
      endTime = prisonAppointment.endTime,
      bookingHistory = bookingHistory,
    )

    bookingHistory.addBookingHistoryAppointments(listOf(bookingHistoryAppointment))

    return bookingHistory
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
